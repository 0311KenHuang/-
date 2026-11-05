package com.envmon.dao.scope;

import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 时序查询守卫的单测：只验证判定分支，不连数据库。
 *
 * 重点覆盖 fail-closed 的几条路径——它们才是"新增查询入口漏判"时的兜底。
 */
class TsScopeGuardTest {

    private static final Long TENANT = 1L;
    private static final Long STATION = 100L;

    private final StationScopeRegistry registry = mock(StationScopeRegistry.class);
    private final TsScopeGuard guard = new TsScopeGuard(registry);
    private final Executor executor = mock(Executor.class);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    private Invocation invocation(String statementId, SqlCommandType type, Object param) throws Exception {
        MappedStatement ms = statement(statementId, type);
        when(executor.query(any(MappedStatement.class), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        return new Invocation(executor,
                Executor.class.getMethod("query", MappedStatement.class, Object.class,
                        RowBounds.class, ResultHandler.class),
                new Object[]{ms, param, RowBounds.DEFAULT, null});
    }

    private MappedStatement statement(String id, SqlCommandType type) {
        Configuration configuration = new Configuration();
        SqlSource sqlSource = new SqlSource() {
            @Override
            public BoundSql getBoundSql(Object parameterObject) {
                return new BoundSql(configuration, "select 1", Collections.emptyList(), parameterObject);
            }
        };
        return new MappedStatement.Builder(configuration, id, sqlSource, type).build();
    }

    /**
     * 用真实的 MapperMethod.ParamMap 而不是普通 HashMap：
     * 它重写了 get()，键不存在会抛 BindingException——守卫必须用 containsKey 取。
     */
    private Map<String, Object> params(Object... kv) {
        Map<String, Object> map = new org.apache.ibatis.binding.MapperMethod.ParamMap<Object>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    private TenantContext ctx(String scope, Long userId, Long orgId, Long regionId) {
        TenantContext ctx = new TenantContext();
        ctx.setTenantId(TENANT);
        ctx.setDataScope(scope);
        ctx.setUserId(userId);
        ctx.setOrgId(orgId);
        ctx.setRegionId(regionId);
        TenantContextHolder.set(ctx);
        return ctx;
    }

    private StationScope station(Long opsOrgId, Long enterpriseId, Long regionId, Long engineerId) {
        return new StationScope(STATION, TENANT, opsOrgId, enterpriseId, regionId, engineerId);
    }

    private int codeOf(Throwable e) {
        return ((BizException) e).getCode();
    }

    // ---------- 不需要拦截的情况 ----------

    @Test
    void shouldPassThroughNonTsMapper() throws Throwable {
        // md_station 等业务表本身就是租户插件保护的对象，不归这里管
        Object result = guard.intercept(invocation(
                "com.envmon.dao.mapper.MdStationMapper.selectById", SqlCommandType.SELECT, params("id", STATION)));
        assertSame(Collections.emptyList(), result);
    }

    @Test
    void shouldPassThroughInsertOnTsTable() throws Throwable {
        // 写入方向由接入层显式带租户，不在查询守卫范围内
        guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataMinuteMapper.upsertBatch", SqlCommandType.INSERT,
                params("list", Collections.emptyList())));
    }

    // ---------- 无上下文：只放行全局聚合 ----------

    @Test
    void shouldPassGlobalAggregationWithoutContext() throws Throwable {
        // TsAggregateJob 的跨租户聚合：没有登录态，也没有 scope 参数
        guard.intercept(invocation("com.envmon.dao.mapper.TsDataMinuteMapper.aggregateByHour",
                SqlCommandType.SELECT, params("from", null, "to", null, "stationId", null)));
    }

    @Test
    void shouldRejectStationQueryWithoutContext() throws Exception {
        // 没有登录态却查具体站房：说明上下文丢了，不能放行
        Exception e = assertThrows(BizException.class, () -> guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataMinuteMapper.selectLatestPerFactor",
                SqlCommandType.SELECT, params("stationId", STATION, "since", null))));
        assertEquals(ErrorCode.TENANT_MISSING.getCode(), codeOf(e));
    }

    // ---------- 租户维度 ----------

    @Test
    void shouldRejectTenantMismatch() {
        ctx("ALL", 1L, null, null);
        Exception e = assertThrows(BizException.class, () -> guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataMinuteMapper.selectOverLimit", SqlCommandType.SELECT,
                params("tenantId", 2L, "from", null, "to", null))));
        assertEquals(ErrorCode.DATA_SCOPE_DENIED.getCode(), codeOf(e));
    }

    @Test
    void shouldAllowMatchingTenant() throws Throwable {
        ctx("ALL", 1L, null, null);
        guard.intercept(invocation("com.envmon.dao.mapper.TsDataMinuteMapper.selectOverLimit",
                SqlCommandType.SELECT, params("tenantId", TENANT, "from", null, "to", null)));
    }

    // ---------- 必须声明判定依据（fail-closed） ----------

    @Test
    void shouldRejectWhenNoScopeParameter() {
        ctx("ALL", 1L, null, null);
        Exception e = assertThrows(BizException.class, () -> guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataMinuteMapper.selectFlagged", SqlCommandType.SELECT,
                params("from", null, "to", null))));
        assertEquals(ErrorCode.DATA_SCOPE_DENIED.getCode(), codeOf(e));
    }

    @Test
    void shouldRejectSingleUnnamedParameter() {
        ctx("ALL", 1L, null, null);
        // 无 @Param 的裸参数：拿不到名字就无法判权，直接拒绝
        Exception e = assertThrows(BizException.class, () -> guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataDayMapper.selectById", SqlCommandType.SELECT, STATION)));
        assertEquals(ErrorCode.DATA_SCOPE_DENIED.getCode(), codeOf(e));
    }

    // ---------- 站房维度 + dataScope ----------

    @Test
    void shouldRejectUnknownStation() {
        ctx("ALL", 1L, null, null);
        when(registry.find(TENANT, STATION)).thenReturn(null);
        Exception e = assertThrows(BizException.class, () -> guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataMinuteMapper.selectLatestPerFactor", SqlCommandType.SELECT,
                params("stationId", STATION))));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), codeOf(e));
    }

    @Test
    void shouldAllowStationForAllScope() throws Throwable {
        ctx("ALL", 1L, null, null);
        when(registry.find(TENANT, STATION)).thenReturn(station(null, null, null, 9L));
        guard.intercept(invocation("com.envmon.dao.mapper.TsDataMinuteMapper.selectLatestPerFactor",
                SqlCommandType.SELECT, params("stationId", STATION)));
    }

    @Test
    void shouldAllowSelfScopeWhenEngineerMatches() throws Throwable {
        ctx("SELF", 7L, null, null);
        when(registry.find(TENANT, STATION)).thenReturn(station(null, null, null, 7L));
        guard.intercept(invocation("com.envmon.dao.mapper.TsDataHourMapper.statByFactor",
                SqlCommandType.SELECT, params("stationId", STATION)));
    }

    @Test
    void shouldRejectSelfScopeWhenEngineerDiffers() {
        ctx("SELF", 7L, null, null);
        when(registry.find(TENANT, STATION)).thenReturn(station(null, null, null, 8L));
        Exception e = assertThrows(BizException.class, () -> guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataHourMapper.statByFactor", SqlCommandType.SELECT,
                params("stationId", STATION))));
        assertEquals(ErrorCode.DATA_SCOPE_DENIED.getCode(), codeOf(e));
    }

    @Test
    void shouldRejectOrgScopeWhenStationBelongsToOtherOrg() {
        ctx("ORG", 3L, 30L, null);
        when(registry.find(TENANT, STATION)).thenReturn(station(31L, 41L, null, null));
        Exception e = assertThrows(BizException.class, () -> guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataDayMapper.selectByStationRange", SqlCommandType.SELECT,
                params("stationId", STATION))));
        assertEquals(ErrorCode.DATA_SCOPE_DENIED.getCode(), codeOf(e));
    }

    @Test
    void shouldRejectStationOfAnotherTenant() {
        // 注册表按 tenantId 过滤：查别的租户的站房返回 null
        ctx("ALL", 1L, null, null);
        when(registry.find(TENANT, STATION)).thenReturn(null);
        Exception e = assertThrows(BizException.class, () -> guard.intercept(invocation(
                "com.envmon.dao.mapper.TsDataDayMapper.selectByStationRange", SqlCommandType.SELECT,
                params("stationId", STATION))));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), codeOf(e));
    }

    @Test
    void shouldGuardAllRegisteredMappers() {
        // 三个时序 Mapper 都必须被登记，防止新增 Mapper 时漏掉守卫
        assertEquals(3, TsScopeGuard.GUARDED_MAPPERS.length);
    }

    /** 防止有人把 6 参数重载的拦截签名删掉：CachingExecutor 走的是那一条 */
    @Test
    void shouldDeclareBothQuerySignatures() throws Exception {
        org.apache.ibatis.plugin.Intercepts intercepts =
                TsScopeGuard.class.getAnnotation(org.apache.ibatis.plugin.Intercepts.class);
        assertEquals(2, intercepts.value().length);
        assertEquals(6, intercepts.value()[1].args().length);
        assertEquals(CacheKey.class, intercepts.value()[1].args()[4]);
    }
}
