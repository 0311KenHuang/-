package com.envmon.dao.scope;

import com.envmon.common.context.DataScopeRules;
import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Map;

/**
 * 时序表查询守卫：把数据作用域校验从"每个 Service 方法自觉"下沉到 Mapper 层。
 *
 * 背景：ts_data_minute/hour/day 三张表没有租户插件保护（接入进程无登录态，
 * 插件会误加 tenant_id），防线此前只有一句人肉约定——"每个查询入口先调 requireStation"。
 * 新增一个入口漏调就是跨租户/跨组织泄露，评审很难发现。
 *
 * 现在改成：任何发往这三张表的 SELECT 必须显式声明判定依据，否则直接拒绝。
 *   - 带 stationId  -> 查站房归属，做租户一致 + dataScope 判定（规则与 Service 层同源）
 *   - 带 tenantId   -> 必须等于当前上下文租户（跨站房聚合）
 *   - 两个都没有    -> 拒绝：说明这条 SQL 无从判定范围
 *   - 无上下文      -> 仅允许"无 scope 参数"的全局聚合（定时任务专用），其余拒绝
 *
 * 这是 fail-closed 设计：宁可让新写的查询在自测时报错，也不让它在生产静默越权。
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class})
})
public class TsScopeGuard implements Interceptor {

    /**
     * 受保护的 Mapper 前缀。
     *
     * 新增时序表 Mapper（例如 tdengine 迁移后的新表）时，必须在这里登记，
     * 否则该表重新回到"约定式防线"。TsScopeGuardTest 会校验登记的完整性。
     */
    static final String[] GUARDED_MAPPERS = {
            "com.envmon.dao.mapper.TsDataMinuteMapper.",
            "com.envmon.dao.mapper.TsDataHourMapper.",
            "com.envmon.dao.mapper.TsDataDayMapper."
    };

    private final StationScopeRegistry registry;

    public TsScopeGuard(StationScopeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        if (guarded(ms)) {
            assertScoped(ms, args.length > 1 ? args[1] : null);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /** 只拦 SELECT：写入的方向由接入层自己带租户，不做查询判权 */
    static boolean guarded(MappedStatement ms) {
        if (ms == null || ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return false;
        }
        String id = ms.getId();
        if (id == null) {
            return false;
        }
        for (String prefix : GUARDED_MAPPERS) {
            if (id.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void assertScoped(MappedStatement ms, Object param) {
        String shortId = shortName(ms.getId());
        Map<String, Object> params = asParamMap(param);
        if (params == null) {
            // 单参数/无 @Param：拿不到命名参数，也就无法判定范围
            throw BizException.of(ErrorCode.DATA_SCOPE_DENIED,
                    "时序查询 " + shortId + " 必须使用 @Param 声明 stationId 或 tenantId");
        }
        // 注意：MyBatis 的 ParamMap 重写了 get，键不存在会直接抛 BindingException，
        // 必须先 containsKey 再取（不是普通 HashMap 语义）
        Long stationId = value(params, "stationId");
        Long tenantId = value(params, "tenantId");

        TenantContext ctx = TenantContextHolder.get();
        if (ctx == null || ctx.getTenantId() == null) {
            // 系统上下文（定时任务）：只放行跨租户的全局聚合，
            // 带 scope 参数却没有登录态一律拒绝
            if (stationId != null || tenantId != null) {
                throw BizException.of(ErrorCode.TENANT_MISSING,
                        "时序查询 " + shortId + " 缺少租户上下文");
            }
            return;
        }

        if (tenantId != null && !tenantId.equals(ctx.getTenantId())) {
            throw BizException.of(ErrorCode.DATA_SCOPE_DENIED,
                    "时序查询 " + shortId + " 的 tenantId 与当前租户不一致");
        }
        if (stationId == null) {
            if (tenantId != null) {
                return;
            }
            throw BizException.of(ErrorCode.DATA_SCOPE_DENIED,
                    "时序查询 " + shortId + " 必须带 stationId 或 tenantId，无法判定数据范围");
        }

        StationScope scope = registry.find(ctx.getTenantId(), stationId);
        if (scope == null) {
            // 不存在、已删除、或属于其他租户：对外统一按"无权限"返回，不泄露存在性
            throw BizException.of(ErrorCode.NOT_FOUND, "站房不存在或无权限");
        }
        if (!DataScopeRules.allows(ctx, scope.getOpsOrgId(), scope.getEnterpriseId(),
                scope.getRegionId(), scope.getOpsEngineerId())) {
            throw BizException.of(ErrorCode.DATA_SCOPE_DENIED, "超出数据权限范围");
        }
    }

    /**
     * MyBatis 把 @Param 方法参数包装成 ParamMap（含 param1/param2 别名）；
     * 单参数且无注解时是裸对象，这里返回 null 表示"无法按名取值"。
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> asParamMap(Object param) {
        if (param instanceof Map) {
            return (Map<String, Object>) param;
        }
        return null;
    }

    static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 按名取参数值；键不存在返回 null（ParamMap 的 get 会抛异常，不能直接用） */
    static Long value(Map<String, Object> params, String key) {
        return params.containsKey(key) ? asLong(params.get(key)) : null;
    }

    static String shortName(String statementId) {
        int idx = statementId == null ? -1 : statementId.lastIndexOf('.');
        return idx < 0 ? String.valueOf(statementId) : statementId.substring(idx + 1);
    }
}
