package com.envmon.ts.service;

import com.envmon.dao.entity.MdStationFactor;
import com.envmon.dao.entity.TsDataMinute;
import com.envmon.dao.mapper.MdStationFactorMapper;
import com.envmon.dao.mapper.TsDataMinuteMapper;
import com.envmon.ts.calc.TsProperties;
import com.envmon.ts.calc.ValueRules;
import com.envmon.ts.parse.CpSample;
import com.envmon.ts.parse.FactorReading;
import com.envmon.ts.parse.Hj212DataParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 分钟数据落库：CP → 纵向记录。
 *
 * 一个 CP 里同一分钟可能有多个因子，落库后是"一站一因子一分钟"一行，
 * 业务侧才能按 factor_code 直接查历史曲线与有效率。
 *
 * 写库前做附录 G 的计算前置处理（超量程夹取 / 负值修约），
 * 但只改统计口径值，原始报文永远保留在 hj212_frame。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinuteDataService {

    /** 量程配置缓存 10 分钟：因子档案改完最多等一个周期自动生效 */
    private static final long RANGE_TTL_MS = TimeUnit.MINUTES.toMillis(10);

    /** 单条 upsert 最多带多少行：MySQL 预编译占位符上限 65535，留足余量 */
    private static final int UPSERT_BATCH = 500;

    private final TsDataMinuteMapper minuteMapper;
    private final MdStationFactorMapper factorMapper;
    private final TsProperties props;

    private final Map<Long, RangeCache> rangeCache = new ConcurrentHashMap<Long, RangeCache>();

    /**
     * 接入层入口：解析 CP 原文并幂等写入。
     *
     * @return 实际写入（含覆盖）的分钟记录数
     */
    public int accept(Long stationId, Long tenantId, String cn, String cp, String qn,
                      Long frameId, LocalDateTime receivedAt) {
        if (stationId == null || !Hj212DataParser.isMinuteData(cn)) {
            return 0;
        }
        return write(stationId, tenantId, cn, Hj212DataParser.parse(cp, qn, receivedAt), frameId);
    }

    /** 历史报文兜底：只有 params 键值对时（老数据没有 CP 原文） */
    public int acceptParams(Long stationId, Long tenantId, String cn, Map<String, String> params,
                            String qn, Long frameId, LocalDateTime receivedAt) {
        if (stationId == null || !Hj212DataParser.isMinuteData(cn)) {
            return 0;
        }
        return write(stationId, tenantId, cn, Hj212DataParser.parseParams(params, qn, receivedAt), frameId);
    }

    /** 因子档案变更后调用，避免继续用旧量程夹取 */
    public void evictStation(Long stationId) {
        if (stationId != null) {
            rangeCache.remove(stationId);
        }
    }

    private int write(Long stationId, Long tenantId, String cn, List<CpSample> samples, Long frameId) {
        if (samples.isEmpty()) {
            return 0;
        }
        Map<String, BigDecimal> rangeMax = rangeMax(stationId);

        // 同一批里重复的 (因子, 时间) 只保留最后一条，避免 upsert 语句里自相覆盖
        Map<String, TsDataMinute> dedup = new LinkedHashMap<String, TsDataMinute>();
        for (CpSample sample : samples) {
            for (Map.Entry<String, FactorReading> e : sample.getFactors().entrySet()) {
                FactorReading r = e.getValue();
                ValueRules.Result adjusted = ValueRules.apply(e.getKey(), r.getValue(), r.getDevFlag(),
                        rangeMax.get(e.getKey()), props.getNegativeFix());
                if (adjusted.getValue() == null && adjusted.getDevFlag() == null) {
                    continue;
                }
                TsDataMinute row = new TsDataMinute();
                row.setTenantId(tenantId == null ? 0L : tenantId);
                row.setStationId(stationId);
                row.setFactorCode(e.getKey());
                row.setTs(sample.getDataTime());
                row.setValue(adjusted.getValue());
                row.setDevFlag(adjusted.getDevFlag());
                row.setQuality(qualityOf(adjusted.getValue(), adjusted.getDevFlag()));
                row.setSourceCn(cn);
                row.setFrameId(frameId);
                dedup.put(stationId + "|" + e.getKey() + "|" + sample.getDataTime(), row);
            }
        }
        if (dedup.isEmpty()) {
            return 0;
        }
        // 补传报文一次可能带几百分钟，按批切，避免触发 MySQL 占位符上限
        int n = 0;
        List<TsDataMinute> rows = new ArrayList<TsDataMinute>(dedup.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH) {
            n += minuteMapper.upsertBatch(rows.subList(i, Math.min(i + UPSERT_BATCH, rows.size())));
        }
        log.debug("分钟数据写入 station={} 条数={}", stationId, n);
        return n;
    }

    /**
     * 附录 G 分钟有效性：
     *   N = 有效；
     *   T = 超量程，值已按量程上限夹取，仍算有效——超量程是量程边界问题，不是故障，
     *       若判无效会让超标时段的有效率凭空掉一截；
     *   D / C / M / Td = 无效（故障 / 校准 / 维护 / 关键工况参数故障）；
     *   无标记 = 未标记，不满足"标记为 N 才有效"。
     */
    public static int qualityOf(BigDecimal value, String devFlag) {
        if (value == null) {
            return 2;
        }
        if (devFlag == null || devFlag.isEmpty()) {
            return 3;
        }
        if ("N".equalsIgnoreCase(devFlag) || "T".equalsIgnoreCase(devFlag)) {
            return 1;
        }
        return 2;
    }

    private Map<String, BigDecimal> rangeMax(Long stationId) {
        long now = System.currentTimeMillis();
        RangeCache cache = rangeCache.get(stationId);
        if (cache != null && now - cache.loadedAt <= RANGE_TTL_MS) {
            return cache.maxByFactor;
        }
        Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
        try {
            List<MdStationFactor> configs = factorMapper.selectByStationIgnoreTenant(stationId);
            if (configs != null) {
                for (MdStationFactor f : configs) {
                    BigDecimal max = toDecimal(f.getRangeMax());
                    if (max != null) {
                        map.put(f.getFactorCode(), max);
                    }
                }
            }
        } catch (Exception e) {
            // 配置读不到就跳过夹取，但不能影响数据落库
            log.warn("量程配置加载失败 station={}: {}", stationId, e.getMessage());
        }
        cache = new RangeCache(map, now);
        rangeCache.put(stationId, cache);
        return cache.maxByFactor;
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

    private static final class RangeCache {
        private final Map<String, BigDecimal> maxByFactor;
        private final long loadedAt;

        private RangeCache(Map<String, BigDecimal> maxByFactor, long loadedAt) {
            this.maxByFactor = maxByFactor;
            this.loadedAt = loadedAt;
        }
    }
}
