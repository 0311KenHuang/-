package com.envmon.ingest.service;

import com.envmon.dao.entity.Hj212Frame;
import com.envmon.dao.entity.MdDevice;
import com.envmon.dao.mapper.Hj212FrameMapper;
import com.envmon.dao.mapper.MdDeviceMapper;
import com.envmon.ingest.hj212.Hj212Message;
import com.envmon.ts.service.DeviceParamService;
import com.envmon.ts.service.MinuteDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 报文落库 + MN 归属解析。
 *
 * 直连场景下，MN 是唯一能把报文和站房对上号的字段：
 *   数采仪上报 MN → md_device.mn → station_id
 * 未注册的 MN 一律视为非法接入，落原文但不归属站房，并计数告警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FrameService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Hj212FrameMapper frameMapper;
    private final MdDeviceMapper deviceMapper;
    private final MinuteDataService minuteDataService;
    private final DeviceParamService deviceParamService;

    /** 工作参数类报文的 CN；2025 正式编码未固定，按现场厂商约定配置 */
    @Value("${hj212.work-param-cn:3020,3031,3041}")
    private String workParamCn;

    /** MN 缓存有效期：到期重查库。设备改绑/删除 MN 后最多这么久自动收敛 */
    @Value("${hj212.mn-cache-ttl-seconds:600}")
    private long mnCacheTtlSeconds;

    /** MN → 缓存条目（归属 + 写入时间），避免每条报文都查库 */
    private final Map<String, CacheEntry> mnCache = new ConcurrentHashMap<String, CacheEntry>();

    /** 单条缓存 */
    private static final class CacheEntry {
        final long[] owner;
        final long cachedAtMillis;

        CacheEntry(long[] owner) {
            this.owner = owner;
            this.cachedAtMillis = System.currentTimeMillis();
        }

        boolean expired(long ttlMillis) {
            return System.currentTimeMillis() - cachedAtMillis > ttlMillis;
        }
    }

    public final AtomicLong received = new AtomicLong();
    public final AtomicLong accepted = new AtomicLong();
    public final AtomicLong crcFailed = new AtomicLong();
    public final AtomicLong parseFailed = new AtomicLong();
    public final AtomicLong unknownMn = new AtomicLong();
    /** 解析写入的分钟记录数（含覆盖） */
    public final AtomicLong minuteRows = new AtomicLong();

    /**
     * 按 MN 解析归属站房。
     *
     * @return long[]{stationId, tenantId}；未注册返回 null
     */
    public long[] resolve(String mn) {
        if (mn == null || mn.isEmpty()) {
            return null;
        }
        long ttlMillis = Math.max(30L, mnCacheTtlSeconds) * 1000L;
        CacheEntry cached = mnCache.get(mn);
        if (cached != null && !cached.expired(ttlMillis)) {
            return cached.owner;
        }
        if (cached != null) {
            // 过期先移除，并发下可能重复查库一次，可接受
            mnCache.remove(mn, cached);
        }
        MdDevice device = deviceMapper.selectByMnIgnoreTenant(mn);
        if (device == null || device.getStationId() == null) {
            unknownMn.incrementAndGet();
            return null;
        }
        CacheEntry hit = new CacheEntry(new long[]{device.getStationId(),
                device.getTenantId() == null ? 0L : device.getTenantId()});
        mnCache.put(mn, hit);
        return hit.owner;
    }

    public void persist(Hj212Message m, String source, long[] owner) {
        Hj212Frame row = new Hj212Frame();
        row.setMn(m.getMn());
        row.setCn(m.getCn());
        row.setQn(m.getQn());
        row.setStationId(owner == null ? null : owner[0]);
        row.setCrcOk(m.isCrcOk() ? 1 : 0);
        row.setSource(source);
        row.setRaw(m.getRaw());
        row.setParams(toJson(m));
        row.setCp(m.getCp());
        row.setReceivedAt(LocalDateTime.now());
        frameMapper.insert(row);

        // 解析产物独立落库：分钟表写失败不能影响原文留存与设备应答，
        // 后续可以用 TsBackfillService 从原始报文重算，不会丢数。
        if (owner != null) {
            try {
                int n = minuteDataService.accept(owner[0], owner[1], m.getCn(), m.getCp(),
                        m.getQn(), row.getId(), row.getReceivedAt());
                if (n > 0) {
                    minuteRows.addAndGet(n);
                }
            } catch (Exception e) {
                log.error("分钟数据写入失败 MN={} CN={}: {}", m.getMn(), m.getCn(), e.getMessage());
            }
            if (isWorkParamFrame(m.getCn())) {
                try {
                    deviceParamService.accept(owner[0], owner[1], m.getMn(), m.getCp(),
                            m.getQn(), row.getId(), row.getReceivedAt());
                } catch (Exception e) {
                    log.error("工作参数写入失败 MN={} CN={}: {}", m.getMn(), m.getCn(), e.getMessage());
                }
            }
        }
    }

    private boolean isWorkParamFrame(String cn) {
        if (cn == null || workParamCn == null) {
            return false;
        }
        Set<String> codes = new HashSet<String>(Arrays.asList(workParamCn.split(",")));
        return codes.contains(cn.trim());
    }

    /** 设备增删改后应调用，避免缓存与库不一致 */
    public void evict(String mn) {
        if (mn != null) {
            mnCache.remove(mn);
        }
    }

    public void evictAll() {
        mnCache.clear();
    }

    public int cachedMnCount() {
        return mnCache.size();
    }

    /** 当前配置的缓存 TTL（秒），供运维接口观测 */
    public long mnCacheTtlSeconds() {
        return Math.max(30L, mnCacheTtlSeconds);
    }

    private String toJson(Hj212Message m) {
        try {
            return MAPPER.writeValueAsString(m.getParams());
        } catch (Exception e) {
            return null;
        }
    }
}
