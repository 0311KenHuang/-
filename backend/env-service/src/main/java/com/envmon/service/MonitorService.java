package com.envmon.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.envmon.dao.entity.AlmEvent;
import com.envmon.dao.entity.Hj212Frame;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.dao.mapper.Hj212FrameMapper;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.service.datascope.DataScopeService;
import com.envmon.ts.service.TsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 工作台概览：先满足看板，后续再上预聚合表 */
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MdStationMapper stationMapper;
    private final AlmEventMapper alarmMapper;
    private final Hj212FrameMapper frameMapper;
    private final TsQueryService tsQueryService;
    private final DataScopeService dataScopeService;

    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<String, Object>();

        QueryWrapper<MdStation> group = new QueryWrapper<MdStation>();
        // dataScope 落地：ORG/SELF 用户的看板只统计自己可见的站房
        List<Long> visible = dataScopeService.visibleStationIds();
        if (visible != null) {
            if (visible.isEmpty()) {
                group.apply("1 = 0");
            } else {
                group.in("id", visible);
            }
        }
        group.select("biz_status AS status", "COUNT(1) AS cnt").groupBy("biz_status");
        List<Map<String, Object>> rows = stationMapper.selectMaps(group);

        Map<String, Long> byStatus = new HashMap<String, Long>();
        long total = 0L;
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            if (status == null) {
                continue;
            }
            long n = cnt == null ? 0L : Long.parseLong(String.valueOf(cnt));
            byStatus.put(String.valueOf(status), n);
            total += n;
        }

        QueryWrapper<AlmEvent> alarmAll = new QueryWrapper<AlmEvent>();
        QueryWrapper<AlmEvent> alarmOpen = new QueryWrapper<AlmEvent>()
                .in("status", "OPEN", "ACK", "PROCESSING");
        if (visible != null) {
            // 报警计数与站房口径一致：只算可见站房的事件
            if (visible.isEmpty()) {
                alarmAll.apply("1 = 0");
                alarmOpen.apply("1 = 0");
            } else {
                alarmAll.in("station_id", visible);
                alarmOpen.in("station_id", visible);
            }
        }
        Long alarms = alarmMapper.selectCount(alarmAll);
        Long openAlarms = alarmMapper.selectCount(alarmOpen);
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        long frames = frameMapper.countSince(dayStart);

        Hj212Frame last = frameMapper.selectOne(new QueryWrapper<Hj212Frame>()
                .orderByDesc("received_at").last("LIMIT 1"));

        // 今日分钟数据量与有效率：工作台的"数据有效率"必须有真实分母，
        // 不能再用"在线站房比例"冒充。
        Map<String, Object> minuteStats = tsQueryService.minuteStats(dayStart, dayStart.plusDays(1));

        result.put("stationTotal", total);
        result.put("stationByStatus", byStatus);
        result.put("alarmTotal", alarms == null ? 0L : alarms);
        result.put("alarmOpenTotal", openAlarms == null ? 0L : openAlarms);
        result.put("frameToday", frames);
        result.put("lastFrameAt", last == null ? null : last.getReceivedAt());
        result.put("minuteToday", minuteStats.get("totalCnt"));
        result.put("minuteValidRate", minuteStats.get("validRate"));
        return result;
    }
}
