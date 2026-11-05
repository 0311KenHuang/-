package com.envmon.service.alarm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envmon.common.api.PageResult;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.AlmAction;
import com.envmon.dao.entity.AlmEvent;
import com.envmon.dao.entity.AlmRule;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.mapper.AlmActionMapper;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.dao.mapper.AlmRuleMapper;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.service.alarm.dto.AlarmEventQuery;
import com.envmon.service.alarm.dto.AlarmEventView;
import com.envmon.service.datascope.DataScopeService;
import com.envmon.service.notify.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 报警事件查询与处置：ACK / CLOSE / FALSE_ALARM 都要留痕 */
@Service
@RequiredArgsConstructor
public class AlarmEventService {

    private final AlmEventMapper eventMapper;
    private final AlmRuleMapper ruleMapper;
    private final AlmActionMapper actionMapper;
    private final MdStationMapper stationMapper;
    private final NotificationService notificationService;
    private final DataScopeService dataScopeService;

    public PageResult<AlarmEventView> page(AlarmEventQuery q) {
        int pageNo = q.getPage() <= 0 ? 1 : q.getPage();
        int size = q.getPageSize() <= 0 ? 20 : Math.min(q.getPageSize(), 200);

        LambdaQueryWrapper<AlmEvent> w = new LambdaQueryWrapper<AlmEvent>();
        // dataScope 落地：报警事件按可见站房过滤（null = ALL 不限制）
        List<Long> visible = dataScopeService.visibleStationIds();
        if (visible != null) {
            if (visible.isEmpty()) {
                return PageResult.of(Collections.<AlarmEventView>emptyList(), 0, pageNo, size);
            }
            w.in(AlmEvent::getStationId, visible);
        }
        if (StringUtils.isNotBlank(q.getStatus())) {
            w.eq(AlmEvent::getStatus, q.getStatus().trim());
        }
        if (StringUtils.isNotBlank(q.getLevel())) {
            w.eq(AlmEvent::getLevel, q.getLevel().trim());
        }
        if (q.getStationId() != null) {
            w.eq(AlmEvent::getStationId, q.getStationId());
        }
        w.orderByDesc(AlmEvent::getBeginAt);

        Page<AlmEvent> p = eventMapper.selectPage(new Page<AlmEvent>(pageNo, size), w);
        return PageResult.of(toViews(p.getRecords()), p.getTotal(), pageNo, size);
    }

    /**
     * 处置报警。
     *
     * ACK 只认领不改状态语义；CLOSE / FALSE_ALARM 结束事件。
     * 已结束的事件不允许再改，避免"事后翻案"。
     */
    @Transactional(rollbackFor = Exception.class)
    public AlarmEventView handle(Long id, String action, String remark) {
        AlmEvent event = id == null ? null : eventMapper.selectById(id);
        if (event == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "报警事件不存在");
        }
        String act = action == null ? "" : action.trim().toUpperCase();
        if (!"ACK".equals(act) && !"CLOSE".equals(act) && !"FALSE_ALARM".equals(act)) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "处置动作不合法");
        }
        if ("CLOSED".equals(event.getStatus()) || "FALSE_ALARM".equals(event.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "事件已结束，不能再处置");
        }

        Long operator = TenantContextHolder.userId();
        if ("ACK".equals(act)) {
            event.setStatus("ACK");
            event.setOwnerId(operator);
        } else {
            event.setStatus("CLOSE".equals(act) ? "CLOSED" : "FALSE_ALARM");
            event.setEndAt(LocalDateTime.now());
        }
        eventMapper.updateById(event);

        AlmAction log = new AlmAction();
        log.setTenantId(TenantContextHolder.tenantId());
        log.setEventId(event.getId());
        log.setAction(act);
        log.setOperator(operator);
        log.setRemark(remark);
        actionMapper.insert(log);
        notificationService.onHandled(event, act);

        return toViews(Collections.singletonList(event)).get(0);
    }

    private List<AlarmEventView> toViews(List<AlmEvent> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> stationIds = new HashSet<Long>();
        Set<Long> ruleIds = new HashSet<Long>();
        for (AlmEvent event : events) {
            if (event.getStationId() != null) {
                stationIds.add(event.getStationId());
            }
            if (event.getRuleId() != null) {
                ruleIds.add(event.getRuleId());
            }
        }
        Map<Long, MdStation> stations = new HashMap<Long, MdStation>();
        if (!stationIds.isEmpty()) {
            for (MdStation station : stationMapper.selectBatchIds(stationIds)) {
                stations.put(station.getId(), station);
            }
        }
        Map<Long, AlmRule> rules = new HashMap<Long, AlmRule>();
        if (!ruleIds.isEmpty()) {
            for (AlmRule rule : ruleMapper.selectBatchIds(ruleIds)) {
                rules.put(rule.getId(), rule);
            }
        }

        List<AlarmEventView> views = new ArrayList<AlarmEventView>(events.size());
        for (AlmEvent event : events) {
            AlarmEventView v = new AlarmEventView();
            v.setId(event.getId());
            v.setEventNo(event.getEventNo());
            v.setRuleId(event.getRuleId());
            v.setStationId(event.getStationId());
            v.setFactorCode(event.getFactorCode());
            v.setLevel(event.getLevel());
            v.setValue(event.getValue());
            v.setThreshold(event.getThreshold());
            v.setBeginAt(event.getBeginAt());
            v.setEndAt(event.getEndAt());
            v.setStatus(event.getStatus());
            v.setOwnerId(event.getOwnerId());

            MdStation station = stations.get(event.getStationId());
            if (station != null) {
                v.setStationCode(station.getStationCode());
                v.setStationName(station.getName());
            }
            AlmRule rule = rules.get(event.getRuleId());
            if (rule != null) {
                v.setRuleCode(rule.getRuleCode());
                v.setRuleName(rule.getName());
            }
            views.add(v);
        }
        return views;
    }
}
