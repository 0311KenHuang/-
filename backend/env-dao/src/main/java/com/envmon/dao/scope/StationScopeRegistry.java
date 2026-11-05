package com.envmon.dao.scope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站房归属注册表：给"时序表查询守卫"提供判权所需的归属字段。
 *
 * 为什么不用 Mapper 查：守卫本身跑在 MyBatis 的 Executor 拦截链里，
 * 再调一次 Mapper 会形成嵌套 SqlSession 查询并多占一条连接——
 * 连接池紧张时可能出现"所有线程都拿着连接等第二条连接"的假死。
 * 这里直接用 JdbcTemplate 读，并且整表快照进内存，判定路径上零 SQL。
 *
 * 一致性取舍：
 *   - 全量快照 TTL 默认 60 秒，站房改绑组织/责任人在 1 分钟内收敛；
 *   - 快照里没有的 id（新建站房、或伪造 id）走一次定向查询再补进快照，
 *     避免"刚建完站房立刻打开实时页 403"；
 *   - 刷新失败沿用旧快照，不让一次数据库抖动把全站页面打成 500。
 */
@Slf4j
@Component
public class StationScopeRegistry {

    /** 只取判定所需字段；deleted = 0 与逻辑删除保持一致 */
    private static final String SQL_ALL =
            "SELECT id, tenant_id, ops_org_id, enterprise_id, region_id, ops_engineer_id "
                    + "FROM md_station WHERE deleted = 0";

    private static final String SQL_ONE = SQL_ALL + " AND id = ?";

    private final JdbcTemplate jdbcTemplate;

    /** 快照有效期：默认 60 秒，可用 envmon.ts.scope-cache-seconds 调整 */
    @Value("${envmon.ts.scope-cache-seconds:60}")
    private int ttlSeconds = 60;

    private volatile Map<Long, StationScope> snapshot = Collections.emptyMap();

    private volatile long loadedAt = 0L;

    private final Object refreshLock = new Object();

    public StationScopeRegistry(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查站房归属。
     *
     * @return null 表示站房不存在、已删除，或不属于给定租户（三种情况一律按"无权限"处理）
     */
    public StationScope find(Long tenantId, Long stationId) {
        if (stationId == null) {
            return null;
        }
        if (isStale()) {
            refresh();
        }
        StationScope scope = snapshot.get(stationId);
        if (scope == null) {
            // 新建站房/未知 id：定向补一条，避免为了一次未命中重载整表
            scope = loadOne(stationId);
            if (scope != null) {
                putOne(scope);
            }
        }
        if (scope == null) {
            return null;
        }
        return tenantId == null || tenantId.equals(scope.getTenantId()) ? scope : null;
    }

    /** 测试与"站房归属变更"后可主动失效 */
    public void invalidate() {
        loadedAt = 0L;
    }

    private boolean isStale() {
        long ttl = Math.max(1, ttlSeconds) * 1000L;
        return System.currentTimeMillis() - loadedAt > ttl;
    }

    private void refresh() {
        synchronized (refreshLock) {
            if (!isStale()) {
                return;
            }
            long now = System.currentTimeMillis();
            try {
                List<StationScope> list = jdbcTemplate.query(SQL_ALL, (rs, i) -> map(rs));
                Map<Long, StationScope> fresh = new HashMap<Long, StationScope>(list.size() * 2);
                for (StationScope scope : list) {
                    fresh.put(scope.getStationId(), scope);
                }
                snapshot = fresh;
                loadedAt = now;
            } catch (Exception e) {
                // 保留旧快照并推迟一个周期再试：判权失败会让所有时序页面不可用，
                // 相比之下"用最多 60 秒前的归属信息"风险更低
                loadedAt = now;
                log.warn("站房归属快照刷新失败，沿用旧快照（{} 条）: {}",
                        snapshot.size(), e.getMessage());
            }
        }
    }

    private StationScope loadOne(Long stationId) {
        try {
            List<StationScope> list = jdbcTemplate.query(SQL_ONE, (rs, i) -> map(rs), stationId);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            log.warn("站房归属定向查询失败 stationId={}: {}", stationId, e.getMessage());
            return null;
        }
    }

    private void putOne(StationScope scope) {
        synchronized (refreshLock) {
            Map<Long, StationScope> copy = new HashMap<Long, StationScope>(snapshot);
            copy.put(scope.getStationId(), scope);
            snapshot = copy;
        }
    }

    private StationScope map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StationScope(
                getLong(rs, "id"),
                getLong(rs, "tenant_id"),
                getLong(rs, "ops_org_id"),
                getLong(rs, "enterprise_id"),
                getLong(rs, "region_id"),
                getLong(rs, "ops_engineer_id"));
    }

    private Long getLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(v);
    }
}
