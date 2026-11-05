package com.envmon.ts.service;

import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.Hj212Frame;
import com.envmon.dao.mapper.Hj212FrameMapper;
import com.envmon.ts.parse.Hj212DataParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史报文重算：接解析功能之前入库的帧，或某天聚合算错了，用它补齐。
 *
 * 只回放分钟类报文（2011/2051/2041），写入幂等；
 * 聚合窗口向两侧各多算一天，因为补传报文的 DataTime 可能早于/晚于接收时间。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TsBackfillService {

    private static final int BATCH = 500;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Hj212FrameMapper frameMapper;
    private final MinuteDataService minuteDataService;
    private final TsAggregateService aggregateService;

    public Map<String, Object> rebuild(Long tenantId, LocalDate from, LocalDate to) {
        if (tenantId == null) {
            throw BizException.of(ErrorCode.TENANT_MISSING);
        }
        LocalDate beginDay = from == null ? LocalDate.now() : from;
        LocalDate endDay = to == null || to.isBefore(beginDay) ? beginDay : to;
        LocalDateTime begin = beginDay.atStartOfDay();
        LocalDateTime end = endDay.plusDays(1).atStartOfDay();

        long afterId = 0L;
        int frames = 0;
        int minutes = 0;
        while (true) {
            List<Hj212Frame> batch = frameMapper.selectReplayable(tenantId, begin, end, afterId, BATCH);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (Hj212Frame frame : batch) {
                afterId = frame.getId();
                frames++;
                try {
                    minutes += replay(frame, tenantId);
                } catch (Exception e) {
                    log.warn("报文回放失败 id={} mn={}: {}", frame.getId(), frame.getMn(), e.getMessage());
                }
            }
            if (batch.size() < BATCH) {
                break;
            }
        }

        int hours = aggregateService.aggregateHours(begin.minusDays(1), end.plusDays(1));
        int days = aggregateService.aggregateDays(beginDay.minusDays(1), endDay.plusDays(2));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("frames", frames);
        result.put("minutes", minutes);
        result.put("hours", hours);
        result.put("days", days);
        result.put("from", beginDay.toString());
        result.put("to", endDay.toString());
        log.info("时序重算完成 {}", result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private int replay(Hj212Frame frame, Long tenantId) throws Exception {
        if (frame.getStationId() == null || !Hj212DataParser.isMinuteData(frame.getCn())) {
            return 0;
        }
        if (StringUtils.isNotBlank(frame.getCp())) {
            return minuteDataService.accept(frame.getStationId(), tenantId, frame.getCn(),
                    frame.getCp(), frame.getQn(), frame.getId(), frame.getReceivedAt());
        }
        // 早期数据没有存 CP 原文，只能退回 params；多 DataTime 的补传报文会被折叠
        if (StringUtils.isBlank(frame.getParams())) {
            return 0;
        }
        Map<String, String> params = MAPPER.readValue(frame.getParams(), LinkedHashMap.class);
        return minuteDataService.acceptParams(frame.getStationId(), tenantId, frame.getCn(),
                params, frame.getQn(), frame.getId(), frame.getReceivedAt());
    }
}
