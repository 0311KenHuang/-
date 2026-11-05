package com.envmon.ts.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.dto.CountStat;
import com.envmon.dao.dto.FactorHourStat;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.entity.MdStationFactor;
import com.envmon.dao.entity.TsDataDay;
import com.envmon.dao.entity.TsDataMinute;
import com.envmon.dao.mapper.MdStationFactorMapper;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.dao.mapper.MdStationStopMapper;
import com.envmon.dao.mapper.TsDataDayMapper;
import com.envmon.dao.mapper.TsDataHourMapper;
import com.envmon.dao.mapper.TsDataMinuteMapper;
import com.envmon.ts.calc.TsProperties;
import com.envmon.ts.dto.RealTimeValue;
import com.envmon.ts.dto.StationRealtime;
import com.envmon.ts.dto.ValidRateItem;
import com.envmon.ts.dto.ValidRateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台查询：站房实时值 + 附录 G 有效率。
 *
 * ts_* 表不受租户插件保护，所以入口统一先按 station_id 查站房：
 * 站房本身受租户约束，查不到就是无权访问，后面的时序查询也就被限定住了。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TsQueryService {

    private final TsDataMinuteMapper minuteMapper;
    private final TsDataHourMapper hourMapper;
    private final TsDataDayMapper dayMapper;
    private final MdStationMapper stationMapper;
    private final MdStationFactorMapper factorMapper;
    private final MdStationStopMapper stopMapper;
    private final TsProperties props;

    /** 跨站房实时值：每站每因子取最新一条 */
    public List<StationRealtime> realtime(int limit) {
        int size = limit <= 0 ? 200 : Math.min(limit, 500);
        LocalDateTime since = LocalDateTime.now().minusHours(Math.max(1, props.getRealtimeWindowHours()));
        List<TsDataMinute> rows = minuteMapper.selectLatestAll(since, TenantContextHolder.tenantId(), size);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<TsDataMinute>> grouped = new LinkedHashMap<Long, List<TsDataMinute>>();
        for (TsDataMinute row : rows) {
            List<TsDataMinute> list = grouped.get(row.getStationId());
            if (list == null) {
                list = new ArrayList<TsDataMinute>();
                grouped.put(row.getStationId(), list);
            }
            list.add(row);
        }

        Map<Long, MdStation> stations = loadStations(grouped.keySet());
        Map<Long, Map<String, MdStationFactor>> configs = loadFactorConfigs(grouped.keySet());

        // dataScope 落地：跨站房列表按作用域逐站断言（规则与 requireStation 同源）
        com.envmon.common.context.TenantContext scopeCtx =
                com.envmon.common.context.TenantContextHolder.get();

        List<StationRealtime> out = new ArrayList<StationRealtime>();
        for (Map.Entry<Long, List<TsDataMinute>> e : grouped.entrySet()) {
            MdStation station = stations.get(e.getKey());
            if (station == null) {
                continue;
            }
            if (!com.envmon.common.context.DataScopeRules.allows(scopeCtx,
                    station.getOpsOrgId(), station.getEnterpriseId(),
                    station.getRegionId(), station.getOpsEngineerId())) {
                continue;
            }
            StationRealtime sr = base(station);
            Map<String, MdStationFactor> cfg = configs.get(e.getKey());
            for (TsDataMinute row : e.getValue()) {
                RealTimeValue v = toValue(row, cfg == null ? null : cfg.get(row.getFactorCode()));
                sr.getFactors().add(v);
                if (sr.getUpdatedAt() == null || row.getTs().isAfter(sr.getUpdatedAt())) {
                    sr.setUpdatedAt(row.getTs());
                }
            }
            out.add(sr);
        }
        return out;
    }

    /** 单站房实时值：配置了但没上报的因子也返回，前端能显示"缺数" */
    public StationRealtime stationRealtime(Long stationId) {
        MdStation station = requireStation(stationId);
        LocalDateTime since = LocalDateTime.now().minusHours(Math.max(1, props.getRealtimeWindowHours()));
        List<TsDataMinute> rows = minuteMapper.selectLatestPerFactor(stationId, since);
        Map<String, TsDataMinute> byFactor = new LinkedHashMap<String, TsDataMinute>();
        if (rows != null) {
            for (TsDataMinute row : rows) {
                byFactor.put(row.getFactorCode(), row);
            }
        }

        StationRealtime sr = base(station);
        List<MdStationFactor> configured = factorMapper.selectList(new QueryWrapper<MdStationFactor>()
                .eq("station_id", stationId)
                .orderByAsc("sort"));
        for (MdStationFactor cfg : configured) {
            TsDataMinute row = byFactor.remove(cfg.getFactorCode());
            RealTimeValue v = row == null ? emptyValue(cfg) : toValue(row, cfg);
            sr.getFactors().add(v);
            if (row != null && (sr.getUpdatedAt() == null || row.getTs().isAfter(sr.getUpdatedAt()))) {
                sr.setUpdatedAt(row.getTs());
            }
        }
        // 有数据但没配在档案里的因子也要暴露出来，否则会怀疑数据丢了
        for (TsDataMinute row : byFactor.values()) {
            RealTimeValue v = toValue(row, null);
            sr.getFactors().add(v);
            if (sr.getUpdatedAt() == null || row.getTs().isAfter(sr.getUpdatedAt())) {
                sr.setUpdatedAt(row.getTs());
            }
        }
        return sr;
    }

    /**
     * 单站房某天有效率。
     *
     * 分母：过去日期 24 小时；当天用已过小时数，否则上午看板上永远是 10% 出头，容易被误判成数据中断。
     */
    public ValidRateResult validRate(Long stationId, LocalDate date) {
        requireStation(stationId);
        LocalDateTime now = LocalDateTime.now();
        LocalDate day = date == null ? LocalDate.now() : date;
        boolean today = day.equals(now.toLocalDate());

        LocalDateTime dayBegin = day.atStartOfDay();
        LocalDateTime dayEnd = dayBegin.plusDays(1);
        // 当天只统计已过小时；停运区间内的时段从分母里扣掉（附录 G 停运不参与统计）
        LocalDateTime statEnd = today ? now : dayEnd;
        int baseHours = today ? Math.max(statEnd.getHour(), 1) : props.getDayHours();
        BigDecimal stopped = stopMapper.stoppedHours(stationId, dayBegin, statEnd);
        int stoppedHours = stopped == null ? 0 : (int) Math.round(stopped.doubleValue());
        stoppedHours = Math.max(0, Math.min(stoppedHours, baseHours));
        int expected = Math.max(baseHours - stoppedHours, 1);
        int required = (int) Math.ceil(expected * props.getDayValidRate().doubleValue());

        List<FactorHourStat> stats = hourMapper.statByFactor(stationId, dayBegin, dayEnd);
        Map<String, FactorHourStat> byFactor = new LinkedHashMap<String, FactorHourStat>();
        if (stats != null) {
            for (FactorHourStat s : stats) {
                byFactor.put(s.getFactorCode(), s);
            }
        }

        ValidRateResult result = new ValidRateResult();
        result.setStationId(stationId);
        result.setDate(day);
        result.setExpectedHours(expected);
        result.setStoppedHours(stoppedHours);
        result.setStopped(stopMapper.selectOpen(stationId) != null);

        List<MdStationFactor> configured = factorMapper.selectList(new QueryWrapper<MdStationFactor>()
                .eq("station_id", stationId)
                .orderByAsc("sort"));
        for (MdStationFactor cfg : configured) {
            FactorHourStat s = byFactor.remove(cfg.getFactorCode());
            result.getFactors().add(toValidRate(cfg.getFactorCode(), cfg.getUnit(), s, expected, required));
        }
        for (FactorHourStat s : byFactor.values()) {
            result.getFactors().add(toValidRate(s.getFactorCode(), null, s, expected, required));
        }
        return result;
    }

    /** 某天已生成的日数据（设备算好但没有？用重算接口生成） */
    public List<TsDataDay> dayData(Long stationId, LocalDate date) {
        requireStation(stationId);
        LocalDate day = date == null ? LocalDate.now() : date;
        return dayMapper.selectByStationRange(stationId,
                day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    /** 工作台 KPI：区间内分钟数据有效率 */
    public Map<String, Object> minuteStats(LocalDateTime from, LocalDateTime to) {
        CountStat stat = minuteMapper.statRange(from, to, TenantContextHolder.tenantId());
        int total = stat == null || stat.getTotalCnt() == null ? 0 : stat.getTotalCnt();
        int valid = stat == null || stat.getValidCnt() == null ? 0 : stat.getValidCnt();
        BigDecimal rate = total == 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(valid).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("totalCnt", total);
        m.put("validCnt", valid);
        m.put("validRate", rate);
        return m;
    }

    private ValidRateItem toValidRate(String factorCode, String unit, FactorHourStat stat,
                                      int expected, int required) {
        int total = stat == null || stat.getTotalHourCnt() == null ? 0 : stat.getTotalHourCnt();
        int valid = stat == null || stat.getValidHourCnt() == null ? 0 : stat.getValidHourCnt();
        BigDecimal rate = BigDecimal.valueOf(valid)
                .divide(BigDecimal.valueOf(expected), 4, RoundingMode.HALF_UP);
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            rate = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        ValidRateItem item = new ValidRateItem();
        item.setFactorCode(factorCode);
        item.setUnit(unit);
        item.setValidHourCnt(valid);
        item.setTotalHourCnt(total);
        item.setValidRate(rate);
        item.setValid(valid >= required);
        item.setAvgValue(stat == null ? null : stat.getAvgValue());
        return item;
    }

    private MdStation requireStation(Long stationId) {
        MdStation station = stationId == null ? null : stationMapper.selectById(stationId);
        if (station == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "站房不存在或无权限");
        }
        // dataScope 落地：ts_* 表不受租户插件保护，租户隔离靠"站房受租户约束"间接成立，
        // 作用域（ORG/SELF/REGION）再由这里统一断言，规则与 env-service 的 DataScopeService 同源
        com.envmon.common.context.TenantContext ctx =
                com.envmon.common.context.TenantContextHolder.get();
        if (!com.envmon.common.context.DataScopeRules.allows(ctx,
                station.getOpsOrgId(), station.getEnterpriseId(),
                station.getRegionId(), station.getOpsEngineerId())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权访问该站房");
        }
        return station;
    }

    private Map<Long, MdStation> loadStations(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MdStation> list = stationMapper.selectBatchIds(ids);
        Map<Long, MdStation> map = new LinkedHashMap<Long, MdStation>();
        for (MdStation s : list) {
            map.put(s.getId(), s);
        }
        return map;
    }

    private Map<Long, Map<String, MdStationFactor>> loadFactorConfigs(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MdStationFactor> list = factorMapper.selectList(new QueryWrapper<MdStationFactor>()
                .in("station_id", ids));
        Map<Long, Map<String, MdStationFactor>> map = new LinkedHashMap<Long, Map<String, MdStationFactor>>();
        for (MdStationFactor f : list) {
            Map<String, MdStationFactor> inner = map.get(f.getStationId());
            if (inner == null) {
                inner = new LinkedHashMap<String, MdStationFactor>();
                map.put(f.getStationId(), inner);
            }
            inner.put(f.getFactorCode(), f);
        }
        return map;
    }

    private StationRealtime base(MdStation station) {
        StationRealtime sr = new StationRealtime();
        sr.setStationId(station.getId());
        sr.setStationCode(station.getStationCode());
        sr.setStationName(station.getName());
        sr.setBizStatus(station.getBizStatus());
        return sr;
    }

    private RealTimeValue toValue(TsDataMinute row, MdStationFactor cfg) {
        RealTimeValue v = new RealTimeValue();
        v.setFactorCode(row.getFactorCode());
        v.setValue(row.getValue());
        v.setTs(row.getTs());
        v.setDevFlag(row.getDevFlag());
        v.setQuality(row.getQuality());
        if (cfg != null) {
            v.setUnit(cfg.getUnit());
            v.setStdLimit(toDecimal(cfg.getStdLimit()));
        }
        return v;
    }

    private RealTimeValue emptyValue(MdStationFactor cfg) {
        RealTimeValue v = new RealTimeValue();
        v.setFactorCode(cfg.getFactorCode());
        v.setUnit(cfg.getUnit());
        v.setStdLimit(toDecimal(cfg.getStdLimit()));
        return v;
    }

    private BigDecimal toDecimal(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
