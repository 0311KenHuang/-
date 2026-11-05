package com.envmon.ts.service;

import com.envmon.dao.dto.HourAggRow;
import com.envmon.dao.dto.MinuteAggRow;
import com.envmon.dao.entity.TsDataDay;
import com.envmon.dao.entity.TsDataHour;
import com.envmon.dao.mapper.TsDataDayMapper;
import com.envmon.dao.mapper.TsDataHourMapper;
import com.envmon.dao.mapper.TsDataMinuteMapper;
import com.envmon.ts.calc.AggregationCalculator;
import com.envmon.ts.calc.TsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 附录 G 聚合：分钟 → 小时 → 日。
 *
 * 幂等：小时/日表都有 (station_id, factor_code, ts) 唯一键，
 * 重算就是覆盖，所以补传/乱序数据晚到只需对区间再跑一次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TsAggregateService {

    /** 单条 upsert 最多带多少行：MySQL 预编译占位符上限 65535，留足余量 */
    private static final int UPSERT_BATCH = 500;

    private final TsDataMinuteMapper minuteMapper;
    private final TsDataHourMapper hourMapper;
    private final TsDataDayMapper dayMapper;
    private final TsProperties props;

    /** 重算 [from, to) 内所有有数据的小时 */
    public int aggregateHours(LocalDateTime from, LocalDateTime to) {
        return aggregateHours(from, to, null);
    }

    /** 只重算某个站房：停运区间变更后即时修正，不牵连其他站房 */
    public int aggregateStationHours(Long stationId, LocalDateTime from, LocalDateTime to) {
        return aggregateHours(from, to, stationId);
    }

    private int aggregateHours(LocalDateTime from, LocalDateTime to, Long stationId) {
        LocalDateTime begin = from.truncatedTo(ChronoUnit.HOURS);
        if (!begin.isBefore(to)) {
            return 0;
        }
        List<MinuteAggRow> rows = minuteMapper.aggregateByHour(begin, to, stationId);
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        List<TsDataHour> hours = new ArrayList<TsDataHour>(rows.size());
        for (MinuteAggRow row : rows) {
            LocalDateTime hourStart = row.getHourTs() == null ? begin : row.getHourTs();
            hours.add(AggregationCalculator.hour(row, hourStart, props));
        }
        int n = 0;
        for (int i = 0; i < hours.size(); i += UPSERT_BATCH) {
            n += hourMapper.upsertBatch(hours.subList(i, Math.min(i + UPSERT_BATCH, hours.size())));
        }
        log.debug("小时值重算 {} ~ {}：{} 条", begin, to, n);
        return n;
    }

    public int aggregateHour(LocalDateTime hourStart) {
        LocalDateTime begin = hourStart.truncatedTo(ChronoUnit.HOURS);
        return aggregateHours(begin, begin.plusHours(1));
    }

    /** 重算某一天：先补小时，再补日 */
    public int aggregateDay(LocalDate date) {
        LocalDateTime begin = date.atStartOfDay();
        int hours = aggregateHours(begin, begin.plusDays(1));
        int days = aggregateDays(date, date.plusDays(1));
        return hours + days;
    }

    /** 重算 [from, toExclusive) 的日数据，按天逐个幂等覆盖 */
    public int aggregateDays(LocalDate from, LocalDate toExclusive) {
        int total = 0;
        for (LocalDate d = from; d.isBefore(toExclusive); d = d.plusDays(1)) {
            LocalDateTime begin = d.atStartOfDay();
            LocalDateTime end = begin.plusDays(1);
            List<HourAggRow> rows = hourMapper.aggregateHours(begin, end);
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            List<TsDataDay> days = new ArrayList<TsDataDay>(rows.size());
            for (HourAggRow row : rows) {
                days.add(AggregationCalculator.day(row, d, props));
            }
            for (int i = 0; i < days.size(); i += UPSERT_BATCH) {
                total += dayMapper.upsertBatch(days.subList(i, Math.min(i + UPSERT_BATCH, days.size())));
            }
        }
        log.debug("日数据重算 {} ~ {}：{} 条", from, toExclusive, total);
        return total;
    }

    /** 定时任务用：重算最近 N 个已完成小时，兜住补传与乱序 */
    public int aggregateRecent(int hours) {
        LocalDateTime currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        int window = hours <= 0 ? 1 : hours;
        return aggregateHours(currentHour.minusHours(window), currentHour);
    }
}
