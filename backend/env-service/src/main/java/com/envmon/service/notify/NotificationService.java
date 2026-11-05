package com.envmon.service.notify;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envmon.common.api.PageResult;
import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.entity.AlmEvent;
import com.envmon.dao.entity.AlmRule;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.entity.MsgSendLog;
import com.envmon.dao.entity.SysTenant;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.dao.mapper.AlmRuleMapper;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.dao.mapper.MsgSendLogMapper;
import com.envmon.dao.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息中心主流程：模板 → 接收人 → 渠道决策 → 发送（重试/降级）→ 留痕。
 *
 * 发送失败绝不影响报警事件本身；未配置的通道记 SKIPPED 而不是"静默成功"；
 * 升级阶段落在 alm_event.escalate_stage，定时任务不会重复推同一级。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MsgSendLogMapper logMapper;
    private final AlmEventMapper eventMapper;
    private final AlmRuleMapper ruleMapper;
    private final MdStationMapper stationMapper;
    private final SysTenantMapper tenantMapper;
    private final MessageTemplateService templateService;
    private final RecipientResolver recipientResolver;
    private final NotifyProperties props;
    private final List<NotifyChannel> channels;

    /** 新报警首报：通知失败只记日志，不能影响报警事件落库 */
    public void onCreated(AlmEvent event) {
        try {
            dispatch(event, NotifyPlanner.eventType(event.getLevel()), 0, false);
        } catch (Exception e) {
            log.error("报警 {} 首报通知失败: {}", event.getEventNo(), e.getMessage(), e);
        }
    }

    /** 条件恢复后自动关单，推一条已恢复 */
    public void onRecovered(AlmEvent event) {
        try {
            dispatch(event, "ALARM_RECOVER", 0, false);
        } catch (Exception e) {
            log.error("报警 {} 恢复通知失败: {}", event.getEventNo(), e.getMessage(), e);
        }
    }

    /** 人工处置只记站内消息，外部通道不重复打扰 */
    public void onHandled(AlmEvent event, String action) {
        try {
            NotifyMessage message = build(event, "ALARM_RECOVER", 0);
            message.setTitle("报警处置：" + action);
            message.setBody("事件 " + message.getEventNo() + " 已由人工处置为 " + action);
            writeLog(event, "INNER", null, "SENT", null, 0, 0, message, 0);
        } catch (Exception e) {
            log.error("报警 {} 处置留痕失败: {}", event.getEventNo(), e.getMessage(), e);
        }
    }

    /** 全租户升级扫描：未认领到达阈值时逐级推送 */
    public Map<String, Object> escalateAll() {
        TenantContext previous = TenantContextHolder.get();
        List<SysTenant> tenants = tenantMapper.selectList(new QueryWrapper<SysTenant>().eq("status", 1));
        int events = 0;
        int sends = 0;
        try {
            for (SysTenant tenant : tenants) {
                TenantContext ctx = new TenantContext();
                ctx.setTenantId(tenant.getId());
                ctx.setTraceId(previous == null ? null : previous.getTraceId());
                TenantContextHolder.set(ctx);
                try {
                    List<AlmEvent> open = eventMapper.selectList(
                            new QueryWrapper<AlmEvent>().eq("status", "OPEN"));
                    LocalDateTime now = LocalDateTime.now();
                    for (AlmEvent event : open == null ? Collections.<AlmEvent>emptyList() : open) {
                        if (event.getBeginAt() == null) {
                            continue;
                        }
                        long minutes = Duration.between(event.getBeginAt(), now).toMinutes();
                        int target = NotifyPlanner.targetStage(event.getLevel(), minutes, props);
                        int current = event.getEscalateStage() == null ? 0 : event.getEscalateStage();
                        if (target <= current) {
                            continue;
                        }
                        for (int stage = current + 1; stage <= target; stage++) {
                            try {
                                dispatch(event, "ALARM_ESCALATE", stage, true);
                                sends++;
                            } catch (Exception e) {
                                log.error("事件 {} T+{} 升级通知失败: {}", event.getEventNo(), stage, e.getMessage());
                            }
                        }
                        // 推过就记阶段，避免单个通道异常导致反复重推
                        event.setEscalateStage(target);
                        eventMapper.updateById(event);
                        events++;
                    }
                } catch (Exception e) {
                    log.error("租户 {} 升级扫描失败: {}", tenant.getId(), e.getMessage(), e);
                } finally {
                    TenantContextHolder.clear();
                }
            }
        } finally {
            if (previous != null) {
                TenantContextHolder.set(previous);
            } else {
                TenantContextHolder.clear();
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("events", events);
        result.put("sends", sends);
        return result;
    }

    /** 通道可用性：给运维页面看"为什么没收到" */
    public Map<String, Object> channelStatus() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (NotifyChannel channel : channels) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("configured", channel.configured());
            if (!channel.configured()) {
                item.put("reason", reason(channel.code()));
            }
            out.put(channel.code(), item);
        }
        return out;
    }

    /** 手工发一条测试卡片，验证通道配置 */
    public Map<String, Object> sendTest(String level) {
        String lv = level == null || level.trim().isEmpty() ? "P1" : level.trim().toUpperCase();
        NotifyMessage message = new NotifyMessage();
        message.setEventType(NotifyPlanner.eventType(lv));
        message.setLevel(lv);
        message.setRuleName("通道连通性测试");
        message.setStationName("演示站房");
        message.setFactorCode("a21026");
        message.setValue(new BigDecimal("42.6"));
        message.setThreshold(new BigDecimal("40"));
        message.setBeginAt(LocalDateTime.now());
        message.setEventNo("TEST");
        message.setStage(0);
        Recipient recipient = recipientResolver.resolve(null, 0);

        int sent = 0;
        for (String code : NotifyPlanner.channels(message.getEventType(), lv,
                LocalDateTime.now().getHour(), 0, props)) {
            NotifyChannel channel = byCode(code);
            if (channel == null) {
                continue;
            }
            templateService.render(message, code);
            if (!channel.configured()) {
                writeLog(null, code, recipient, "SKIPPED", reason(code), 0, 0, message, 0);
                continue;
            }
            NotifyResult result = sendWithRetry(channel, message, recipient);
            writeLog(null, code, recipient, result.isSuccess() ? "SENT" : "FAILED",
                    result.getError(), result.getCostMs(), result.getRetryCnt(), message, 0);
            if (result.isSuccess()) {
                sent++;
            }
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("level", lv);
        out.put("sent", sent);
        return out;
    }

    /** 发送日志分页 */
    public PageResult<MsgSendLog> logs(Long eventId, String status, String channel, int page, int pageSize) {
        int pageNo = page <= 0 ? 1 : page;
        int size = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        LambdaQueryWrapper<MsgSendLog> w = new LambdaQueryWrapper<MsgSendLog>();
        if (eventId != null) {
            w.eq(MsgSendLog::getEventId, eventId);
        }
        if (status != null && !status.trim().isEmpty()) {
            w.eq(MsgSendLog::getStatus, status.trim());
        }
        if (channel != null && !channel.trim().isEmpty()) {
            w.eq(MsgSendLog::getChannel, channel.trim());
        }
        w.orderByDesc(MsgSendLog::getSentAt);
        Page<MsgSendLog> p = logMapper.selectPage(new Page<MsgSendLog>(pageNo, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), pageNo, size);
    }

    private void dispatch(AlmEvent event, String eventType, int stage, boolean escalation) {
        NotifyMessage message = build(event, eventType, stage);
        Recipient recipient = recipientResolver.resolve(event.getStationId(), stage);
        int hour = LocalDateTime.now().getHour();
        List<String> plan = escalation
                ? NotifyPlanner.channels("ALARM_ESCALATE", event.getLevel(), hour, stage, props)
                : NotifyPlanner.channels(eventType, event.getLevel(), hour, stage, props);

        for (String code : plan) {
            NotifyChannel channel = byCode(code);
            if (channel == null) {
                continue;
            }
            templateService.render(message, code);
            if (!channel.configured()) {
                writeLog(event, code, recipient, "SKIPPED", reason(code), 0, 0, message, stage);
                continue;
            }
            NotifyResult result = sendWithRetry(channel, message, recipient);
            if (result.isSkipped()) {
                writeLog(event, code, recipient, "SKIPPED", result.getError(),
                        result.getCostMs(), 0, message, stage);
                continue;
            }
            writeLog(event, code, recipient, result.isSuccess() ? "SENT" : "FAILED",
                    result.getError(), result.getCostMs(), result.getRetryCnt(), message, stage);
            if (!result.isSuccess() && !"INNER".equals(code)) {
                // 外部通道失败 → 站内消息兜底，保证至少有一条可见记录
                writeLog(event, "INNER", recipient, "SENT", "通道 " + code + " 失败后降级",
                        0, 0, message, stage);
            }
        }
    }

    private NotifyResult sendWithRetry(NotifyChannel channel, NotifyMessage message, Recipient recipient) {
        int attempts = 0;
        NotifyResult last = NotifyResult.fail("未执行", 0);
        int limit = Math.max(1, props.getMaxRetry());
        while (attempts < limit) {
            attempts++;
            last = channel.send(message, recipient);
            if (last.isSuccess() || last.isSkipped()) {
                break;
            }
            try {
                Thread.sleep(50L * attempts);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        last.setRetryCnt(attempts - 1);
        return last;
    }

    private NotifyMessage build(AlmEvent event, String eventType, int stage) {
        MdStation station = event.getStationId() == null ? null : stationMapper.selectById(event.getStationId());
        AlmRule rule = event.getRuleId() == null ? null : ruleMapper.selectById(event.getRuleId());

        NotifyMessage message = new NotifyMessage();
        message.setEventId(event.getId());
        message.setEventNo(event.getEventNo());
        message.setEventType(eventType);
        message.setLevel(event.getLevel());
        message.setRuleName(rule == null ? "报警事件" : rule.getName());
        message.setStationId(event.getStationId());
        message.setStationName(station == null ? "站房" + event.getStationId() : station.getName());
        message.setFactorCode(event.getFactorCode());
        message.setValue(event.getValue());
        message.setThreshold(event.getThreshold());
        message.setBeginAt(event.getBeginAt());
        message.setStage(stage);
        return message;
    }

    private void writeLog(AlmEvent event, String channel, Recipient recipient, String status,
                          String error, int costMs, int retryCnt, NotifyMessage message, int stage) {
        MsgSendLog row = new MsgSendLog();
        row.setTenantId(TenantContextHolder.tenantId());
        row.setEventId(event == null ? null : event.getId());
        row.setChannel(channel);
        row.setReceiver(recipient == null ? null : truncate(recipient.display(), 128));
        row.setTemplateCode(message == null ? null : message.getEventType());
        row.setStatus(status);
        row.setStage(stage);
        row.setRetryCnt(retryCnt);
        row.setErrorMsg(truncate(error, 500));
        row.setCostMs(costMs);
        String content = message == null ? null : message.getTitle() + " | " + message.getBody();
        row.setContent(truncate(content, 1000));
        try {
            logMapper.insert(row);
        } catch (Exception e) {
            log.error("消息日志写入失败: {}", e.getMessage());
        }
    }

    private NotifyChannel byCode(String code) {
        for (NotifyChannel channel : channels) {
            if (channel.code().equals(code)) {
                return channel;
            }
        }
        return null;
    }

    private String reason(String code) {
        if ("FEISHU".equals(code)) {
            return "未配置 notify.feishu.webhook-url（或通道已禁用）";
        }
        if ("SMS".equals(code)) {
            return "未配置 notify.sms.gateway-url（或通道已禁用）";
        }
        if ("WXA".equals(code)) {
            return "未配置 notify.wxa.gateway-url（或通道已禁用）";
        }
        return "通道不可用";
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
