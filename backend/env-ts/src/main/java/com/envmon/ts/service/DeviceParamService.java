package com.envmon.ts.service;

import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.entity.AlmEvent;
import com.envmon.dao.entity.AlmRule;
import com.envmon.dao.entity.DevParamChange;
import com.envmon.dao.entity.DevParamSnapshot;
import com.envmon.dao.entity.MdDevice;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.dao.mapper.AlmRuleMapper;
import com.envmon.dao.mapper.DevParamChangeMapper;
import com.envmon.dao.mapper.DevParamSnapshotMapper;
import com.envmon.dao.mapper.MdDeviceMapper;
import com.envmon.ts.parse.Hj212WorkParamParser;
import com.envmon.ts.parse.WorkParamPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工作参数落库：快照只追加，变更单独留痕并产生 P1 报警事件。
 * 接入进程无登录态，这里临时注入只含 tenantId 的上下文，用完还原；
 * 通知由 API 进程的消息中心补发，避免接入层被慢 HTTP 拖住。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceParamService {

    public static final String RULE_CODE = "DEV_PARAM_CHANGE";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long SUPPRESS_SECONDS = 3600L;

    private final DevParamSnapshotMapper snapshotMapper;
    private final DevParamChangeMapper changeMapper;
    private final MdDeviceMapper deviceMapper;
    private final AlmRuleMapper ruleMapper;
    private final AlmEventMapper eventMapper;

    private final AtomicInteger seq = new AtomicInteger();

    /** @return 快照数 / 变更数 / 新增报警数 */
    public Map<String, Object> accept(Long stationId, Long tenantId, String mn, String cp,
                                      String qn, Long frameId, LocalDateTime receivedAt) {
        List<WorkParamPoint> points = Hj212WorkParamParser.parse(cp, qn, receivedAt);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("snapshots", 0);
        result.put("changes", 0);
        result.put("events", 0);
        if (stationId == null || points.isEmpty()) {
            return result;
        }

        TenantContext previous = TenantContextHolder.get();
        TenantContext ctx = new TenantContext();
        ctx.setTenantId(tenantId);
        TenantContextHolder.set(ctx);
        try {
            Long deviceId = resolveDeviceId(mn);
            AlmRule rule = ruleMapper.selectByCode(tenantId, RULE_CODE);
            int snapshots = 0;
            int changes = 0;
            int events = 0;

            for (WorkParamPoint point : points) {
                String last = deviceId == null ? null
                        : snapshotMapper.selectLastValue(deviceId, point.getParamCode());

                DevParamSnapshot snapshot = new DevParamSnapshot();
                snapshot.setTenantId(tenantId);
                snapshot.setStationId(stationId);
                snapshot.setDeviceId(deviceId);
                snapshot.setMn(mn);
                snapshot.setParamCode(point.getParamCode());
                snapshot.setParamValue(point.getParamValue());
                snapshot.setCollectedAt(point.getCollectedAt());
                snapshot.setSource("DTU");
                snapshot.setFrameId(frameId);
                snapshotMapper.insert(snapshot);
                snapshots++;

                if (last == null || last.equals(point.getParamValue())) {
                    continue;
                }
                changes++;
                Long eventId = null;
                if (!suppressed(deviceId, point)) {
                    eventId = createEvent(stationId, tenantId, point, last, rule);
                    events++;
                }
                DevParamChange change = new DevParamChange();
                change.setTenantId(tenantId);
                change.setStationId(stationId);
                change.setDeviceId(deviceId);
                change.setParamCode(point.getParamCode());
                change.setOldValue(last);
                change.setNewValue(point.getParamValue());
                change.setChangeTime(point.getCollectedAt());
                change.setFrameId(frameId);
                change.setEventId(eventId);
                changeMapper.insert(change);
                log.info("工作参数变更 MN={} {} {} -> {} 事件={}", mn, point.getParamCode(),
                        last, point.getParamValue(), eventId);
            }

            result.put("snapshots", snapshots);
            result.put("changes", changes);
            result.put("events", events);
        } finally {
            if (previous != null) {
                TenantContextHolder.set(previous);
            } else {
                TenantContextHolder.clear();
            }
        }
        return result;
    }

    private Long createEvent(Long stationId, Long tenantId, WorkParamPoint point,
                             String oldValue, AlmRule rule) {
        AlmEvent event = new AlmEvent();
        event.setTenantId(tenantId);
        event.setEventNo(nextEventNo());
        event.setRuleId(rule == null || rule.getId() == null ? 0L : rule.getId());
        event.setStationId(stationId);
        event.setFactorCode(point.getParamCode());
        event.setLevel(rule == null ? "P1" : rule.getLevel());
        event.setValue(toNumber(point.getParamValue()));
        event.setBeginAt(point.getCollectedAt());
        event.setStatus("OPEN");
        event.setEscalateStage(0);
        event.setDeleted(0);
        eventMapper.insert(event);
        return event.getId();
    }

    private boolean suppressed(Long deviceId, WorkParamPoint point) {
        if (deviceId == null) {
            return false;
        }
        LocalDateTime last = changeMapper.selectLastChangeTime(deviceId, point.getParamCode());
        return last != null && point.getCollectedAt() != null
                && Duration.between(last, point.getCollectedAt()).getSeconds() < SUPPRESS_SECONDS;
    }

    private Long resolveDeviceId(String mn) {
        if (mn == null || mn.isEmpty()) {
            return null;
        }
        MdDevice device = deviceMapper.selectByMnIgnoreTenant(mn);
        return device == null ? null : device.getId();
    }

    private BigDecimal toNumber(String value) {
        try {
            return value == null ? null : new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nextEventNo() {
        int n = Math.abs(seq.incrementAndGet() % 10000);
        return "AL" + LocalDateTime.now().format(STAMP) + String.format("%04d", n);
    }
}
