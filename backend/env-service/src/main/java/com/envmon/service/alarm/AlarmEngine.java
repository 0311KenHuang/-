package com.envmon.service.alarm;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.dto.AlarmSampleRow;
import com.envmon.dao.dto.LastPointRow;
import com.envmon.dao.entity.AlmAction;
import com.envmon.dao.entity.AlmEvent;
import com.envmon.dao.entity.AlmRule;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.entity.MdStationStop;
import com.envmon.dao.entity.SysTenant;
import com.envmon.dao.mapper.AlmActionMapper;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.dao.mapper.AlmRuleMapper;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.dao.mapper.MdStationStopMapper;
import com.envmon.dao.mapper.SysTenantMapper;
import com.envmon.dao.mapper.TsDataMinuteMapper;
import com.envmon.service.notify.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 报警引擎：数据类三条规则（超标 / 恒值 / 缺数）。
 *
 * 运行在常驻 Web 进程的定时任务里，按租户逐个扫描：
 *   1. 规则从 alm_rule 读，缺哪条内置规则就补哪条（新租户无需手工初始化）；
 *   2. 过去 N 分钟的数据按规则求值，命中后按"活动事件 + 抑制窗口"决定新增还是跳过；
 *   3. 条件恢复后自动关闭数据类事件（运维/监管类规则必须人工关闭，本期不涉及）。
 *
 * 停运站房整站跳过，停运时段的数据也不参与判定——和附录 G 的统计口径保持一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmEngine {

    private static final List<String> ACTIVE_STATUS = Arrays.asList("OPEN", "ACK", "PROCESSING");
    private static final DateTimeFormatter EVENT_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TsDataMinuteMapper minuteMapper;
    private final AlmRuleMapper ruleMapper;
    private final AlmEventMapper eventMapper;
    private final AlmActionMapper actionMapper;
    private final MdStationMapper stationMapper;
    private final MdStationStopMapper stopMapper;
    private final SysTenantMapper tenantMapper;
    private final AlarmProperties props;
    private final NotificationService notificationService;

    private final AtomicInteger eventSeq = new AtomicInteger();

    /** 全租户扫描，返回汇总给手工触发的接口用 */
    public Map<String, Object> scanAllTenants() {
        // 手工触发时运行在请求线程上，必须先保存原上下文；
        // 否则循环结束 clear() 会把当前用户的租户/用户信息抹掉，审计也拿不到人。
        TenantContext previous = TenantContextHolder.get();
        List<SysTenant> tenants = tenantMapper.selectList(new QueryWrapper<SysTenant>().eq("status", 1));
        int scanned = 0;
        int created = 0;
        int closed = 0;
        try {
            for (SysTenant tenant : tenants) {
                TenantContext ctx = new TenantContext();
                ctx.setTenantId(tenant.getId());
                ctx.setTraceId(previous == null ? null : previous.getTraceId());
                TenantContextHolder.set(ctx);
                try {
                    int[] r = evaluateTenant(tenant.getId());
                    scanned++;
                    created += r[0];
                    closed += r[1];
                } catch (Exception e) {
                    // 单个租户失败不能影响其他租户的报警
                    log.error("租户 {} 报警扫描失败: {}", tenant.getId(), e.getMessage(), e);
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
        result.put("tenants", scanned);
        result.put("created", created);
        result.put("closed", closed);
        log.info("报警扫描完成：租户 {} 新增 {} 关闭 {}", scanned, created, closed);
        return result;
    }

    /** 单租户扫描，调用前必须已设置租户上下文 */
    public int[] evaluateTenant(Long tenantId) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<AlmRule> rules = loadRules(tenantId);
        List<AlmRule> enabled = new ArrayList<AlmRule>();
        int maxSuppress = 0;
        for (AlmRule rule : rules) {
            if (rule.getEnabled() != null && rule.getEnabled() == 1) {
                enabled.add(rule);
                maxSuppress = Math.max(maxSuppress, nz(rule.getSuppressSec()));
            }
        }
        if (enabled.isEmpty()) {
            return new int[]{0, 0};
        }

        Set<Long> stopped = stoppedStations(tenantId);
        Map<String, AlmEvent> active = index(eventMapper.selectList(
                new QueryWrapper<AlmEvent>().in("status", ACTIVE_STATUS)));
        Map<String, AlmEvent> recent = maxSuppress <= 0
                ? new HashMap<String, AlmEvent>()
                : index(eventMapper.selectList(new QueryWrapper<AlmEvent>()
                        .ge("begin_at", now.minusSeconds(maxSuppress))));

        Counters counters = new Counters();
        for (AlmRule rule : enabled) {
            AlarmParams params = AlarmParams.parse(rule.getParamsJson());
            try {
                if ("DATA_OVER_LIMIT".equals(rule.getRuleCode())) {
                    evaluateOverLimit(rule, params, now, active, recent, counters);
                } else if ("DATA_DEAD_VALUE".equals(rule.getRuleCode())) {
                    evaluateDeadValue(rule, params, now, active, recent, counters);
                } else if ("DATA_MISSING".equals(rule.getRuleCode())) {
                    evaluateMissing(rule, params, now, stopped, active, recent, counters);
                }
            } catch (Exception e) {
                log.error("规则 {} 执行失败: {}", rule.getRuleCode(), e.getMessage(), e);
            }
        }
        return new int[]{counters.created, counters.closed};
    }

    private void evaluateOverLimit(AlmRule rule, AlarmParams params, LocalDateTime now,
                                   Map<String, AlmEvent> active, Map<String, AlmEvent> recent,
                                   Counters counters) {
        int consecutive = params.getConsecutive() != null
                ? params.getConsecutive()
                : Math.max(1, nz(rule.getDurationSec()) / 60);
        List<AlarmSampleRow> rows = minuteMapper.selectOverLimit(currentTenantId(),
                now.minusMinutes(props.getOverLimitWindowMinutes()), now.plusMinutes(1));
        List<AlarmHit> hits = AlarmDetector.overLimit(rows, consecutive);
        Set<String> hitKeys = raise(rule, hits, active, recent, now, counters);
        closeNotHit(rule, hitKeys, active, now, counters);
    }

    private void evaluateDeadValue(AlmRule rule, AlarmParams params, LocalDateTime now,
                                   Map<String, AlmEvent> active, Map<String, AlmEvent> recent,
                                   Counters counters) {
        int consecutive = params.getConsecutive() == null ? 10 : params.getConsecutive();
        List<AlarmSampleRow> points = minuteMapper.selectRecentPoints(currentTenantId(),
                now.minusMinutes(props.getDeadLookbackMinutes()), now.plusMinutes(1), consecutive);
        List<AlarmHit> hits = AlarmDetector.deadValue(points, consecutive);
        Set<String> hitKeys = raise(rule, hits, active, recent, now, counters);
        closeNotHit(rule, hitKeys, active, now, counters);
    }

    private void evaluateMissing(AlmRule rule, AlarmParams params, LocalDateTime now, Set<Long> stopped,
                                 Map<String, AlmEvent> active, Map<String, AlmEvent> recent,
                                 Counters counters) {
        int gapMin = params.getGapMin() == null ? 30 : params.getGapMin();
        List<LastPointRow> lastPoints = minuteMapper.selectLastPoints(currentTenantId(),
                now.minusDays(props.getMissingLookbackDays()));
        AlarmDetector.MissingResult result = AlarmDetector.missing(lastPoints, now, gapMin, stopped);
        raise(rule, result.getHits(), active, recent, now, counters);
        // 缺数事件只有"数据恢复"或"站房停运"才自动关闭；
        // 回看窗口里查不到数据的 key 不能误判为恢复，否则长期缺数会被悄悄关掉。
        closeKeys(rule, result.getFreshKeys(), active, now, counters);
    }

    private Set<String> raise(AlmRule rule, List<AlarmHit> hits, Map<String, AlmEvent> active,
                              Map<String, AlmEvent> recent, LocalDateTime now, Counters counters) {
        Set<String> keys = new HashSet<String>();
        if (hits == null) {
            return keys;
        }
        for (AlarmHit hit : hits) {
            if (hit.getStationId() == null) {
                continue;
            }
            String key = eventKey(rule.getId(), hit.getStationId(), hit.getFactorCode());
            keys.add(key);

            AlmEvent existing = active.get(key);
            if (existing != null) {
                updateValue(existing, hit);
                continue;
            }
            if (suppressed(rule, recent.get(key), now)) {
                continue;
            }
            AlmEvent event = new AlmEvent();
            event.setTenantId(currentTenantId());
            event.setEventNo(nextEventNo());
            event.setRuleId(rule.getId());
            event.setStationId(hit.getStationId());
            event.setFactorCode(hit.getFactorCode());
            event.setLevel(rule.getLevel());
            event.setValue(hit.getValue());
            event.setThreshold(hit.getThreshold());
            event.setBeginAt(hit.getBeginAt() == null ? now : hit.getBeginAt());
            event.setStatus("OPEN");
            eventMapper.insert(event);
            active.put(key, event);
            counters.created++;
            notificationService.onCreated(event);
            log.info("新增报警 {} 站房={} 因子={} 值={} 限值={}",
                    rule.getRuleCode(), hit.getStationId(), hit.getFactorCode(),
                    hit.getValue(), hit.getThreshold());
        }
        return keys;
    }

    private void updateValue(AlmEvent event, AlarmHit hit) {
        boolean changed = false;
        if (hit.getValue() != null && !same(event.getValue(), hit.getValue())) {
            event.setValue(hit.getValue());
            changed = true;
        }
        if (hit.getThreshold() != null && !same(event.getThreshold(), hit.getThreshold())) {
            event.setThreshold(hit.getThreshold());
            changed = true;
        }
        if (changed) {
            eventMapper.updateById(event);
        }
    }

    private boolean suppressed(AlmRule rule, AlmEvent recent, LocalDateTime now) {
        int window = nz(rule.getSuppressSec());
        return window > 0 && recent != null && recent.getBeginAt() != null
                && recent.getBeginAt().isAfter(now.minusSeconds(window));
    }

    /** 自动恢复：本规则的活动事件若本轮没有命中，就关闭 */
    private void closeNotHit(AlmRule rule, Set<String> hitKeys, Map<String, AlmEvent> active,
                             LocalDateTime now, Counters counters) {
        for (AlmEvent event : new ArrayList<AlmEvent>(active.values())) {
            if (!rule.getId().equals(event.getRuleId())) {
                continue;
            }
            String key = eventKey(rule.getId(), event.getStationId(), event.getFactorCode());
            if (!hitKeys.contains(key)) {
                close(event, now, counters);
                active.remove(key);
            }
        }
    }

    private void closeKeys(AlmRule rule, Set<String> keys, Map<String, AlmEvent> active,
                           LocalDateTime now, Counters counters) {
        for (AlmEvent event : new ArrayList<AlmEvent>(active.values())) {
            if (!rule.getId().equals(event.getRuleId())) {
                continue;
            }
            String key = eventKey(rule.getId(), event.getStationId(), event.getFactorCode());
            if (keys.contains(key)) {
                close(event, now, counters);
                active.remove(key);
            }
        }
    }

    private void close(AlmEvent event, LocalDateTime now, Counters counters) {
        event.setStatus("CLOSED");
        event.setEndAt(now);
        eventMapper.updateById(event);
        actionMapper.insert(action(event.getId(), "AUTO_CLOSE", 0L, "条件恢复，系统自动关闭"));
        notificationService.onRecovered(event);
        counters.closed++;
        log.info("报警自动关闭 {} 站房={} 因子={}", event.getRuleId(), event.getStationId(), event.getFactorCode());
    }

    private AlmAction action(Long eventId, String action, Long operator, String remark) {
        AlmAction a = new AlmAction();
        a.setTenantId(currentTenantId());
        a.setEventId(eventId);
        a.setAction(action);
        a.setOperator(operator == null ? 0L : operator);
        a.setRemark(remark);
        return a;
    }

    private List<AlmRule> loadRules(Long tenantId) {
        Map<String, AlmRule> byCode = new LinkedHashMap<String, AlmRule>();
        List<AlmRule> all = ruleMapper.selectList(null);
        if (all != null) {
            for (AlmRule rule : all) {
                byCode.put(rule.getRuleCode(), rule);
            }
        }
        for (Builtin builtin : Builtin.values()) {
            if (!byCode.containsKey(builtin.code)) {
                AlmRule rule = builtin.toEntity(tenantId);
                ruleMapper.insert(rule);
                byCode.put(builtin.code, rule);
                log.info("租户 {} 补齐内置报警规则 {}", tenantId, builtin.code);
            }
        }
        return new ArrayList<AlmRule>(byCode.values());
    }

    private Set<Long> stoppedStations(Long tenantId) {
        Set<Long> stopped = new HashSet<Long>();
        List<MdStationStop> stops = stopMapper.selectOpenByTenant(tenantId);
        if (stops != null) {
            for (MdStationStop stop : stops) {
                if (stop.getStationId() != null) {
                    stopped.add(stop.getStationId());
                }
            }
        }
        List<MdStation> stations = stationMapper.selectList(
                new QueryWrapper<MdStation>().eq("biz_status", "STOPPED"));
        if (stations != null) {
            for (MdStation station : stations) {
                stopped.add(station.getId());
            }
        }
        return stopped;
    }

    private Map<String, AlmEvent> index(List<AlmEvent> events) {
        Map<String, AlmEvent> map = new HashMap<String, AlmEvent>();
        if (events == null) {
            return map;
        }
        for (AlmEvent event : events) {
            String key = eventKey(event.getRuleId(), event.getStationId(), event.getFactorCode());
            AlmEvent prev = map.get(key);
            if (prev == null || isAfter(event, prev)) {
                map.put(key, event);
            }
        }
        return map;
    }

    private boolean isAfter(AlmEvent a, AlmEvent b) {
        return a.getBeginAt() != null && (b.getBeginAt() == null || a.getBeginAt().isAfter(b.getBeginAt()));
    }

    private String eventKey(Long ruleId, Long stationId, String factorCode) {
        return ruleId + "|" + stationId + "|" + (factorCode == null ? "" : factorCode);
    }

    private boolean same(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) == 0;
    }

    private Long currentTenantId() {
        return TenantContextHolder.tenantId();
    }

    private String nextEventNo() {
        int n = Math.abs(eventSeq.incrementAndGet() % 10000);
        return "AL" + LocalDateTime.now().format(EVENT_TS) + String.format("%04d", n);
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static final class Counters {
        private int created;
        private int closed;
    }

    /** 内置规则默认值：库里没有时自动补齐，参数可再通过 alm_rule 调整 */
    private enum Builtin {
        OVER_LIMIT("DATA_OVER_LIMIT", "污染物超标", "DATA", "P1", 180, 3600, "{\"consecutive\":3}"),
        DEAD_VALUE("DATA_DEAD_VALUE", "数据恒值/保持", "DATA", "P2", 0, 14400, "{\"consecutive\":10}"),
        MISSING("DATA_MISSING", "数据缺失", "DATA", "P1", 0, 3600, "{\"gap_min\":30}"),
        /** 由接入层工作参数变更直接产生事件，不参与定时扫描，只用于补齐规则行与级别 */
        PARAM_CHANGE("DEV_PARAM_CHANGE", "工作参数变更", "DEVICE", "P1", 0, 3600, "{}");

        private final String code;
        private final String name;
        private final String category;
        private final String level;
        private final int durationSec;
        private final int suppressSec;
        private final String paramsJson;

        Builtin(String code, String name, String category, String level,
                int durationSec, int suppressSec, String paramsJson) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.level = level;
            this.durationSec = durationSec;
            this.suppressSec = suppressSec;
            this.paramsJson = paramsJson;
        }

        private AlmRule toEntity(Long tenantId) {
            AlmRule rule = new AlmRule();
            rule.setTenantId(tenantId);
            rule.setRuleCode(code);
            rule.setName(name);
            rule.setCategory(category);
            rule.setLevel(level);
            rule.setDurationSec(durationSec);
            rule.setSuppressSec(suppressSec);
            rule.setParamsJson(paramsJson);
            rule.setChannels("FEISHU");
            rule.setEnabled(1);
            rule.setDeleted(0);
            return rule;
        }
    }
}
