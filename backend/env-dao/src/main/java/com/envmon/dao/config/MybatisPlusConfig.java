package com.envmon.dao.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.scope.StationScopeRegistry;
import com.envmon.dao.scope.TsScopeGuard;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MyBatis-Plus 配置。
 *
 * 租户插件是这套系统最重要的横切能力：所有业务 SQL 自动补 tenant_id，
 * 比"每个查询手写 andWhere"安全得多——漏写一处就是跨租户泄露。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 这些表没有 tenant_id，或接入层没有租户上下文，必须跳过。
     *
     * ts_* 属于后者：分钟数据由接入进程写入，那一侧没有登录态；
     * tenant_id 由报文归属的站房显式带入。查询侧必须自己按 station_id 限定范围
     * （站房本身受租户插件保护），不能指望插件兜底。
     *
     * 这条"必须自己限定"的约束由 {@link TsScopeGuard} 在 Executor 层强制执行，
     * 不再依赖每个 Service 方法自觉调用 requireStation。
     */
    private static final Set<String> IGNORE_TABLES = new HashSet<String>(Arrays.asList(
            "sys_tenant",
            "sys_user_role",
            "sys_audit_log",
            "hj212_frame",
            "ts_data_minute",
            "ts_data_hour",
            "ts_data_day",
            "md_station_stop"
    ));

    /**
     * 时序查询守卫：ts_* 的 SELECT 必须显式带 stationId / tenantId，否则 fail-closed。
     *
     * 声明成 Bean 即可被 mybatis(-plus)-spring-boot-starter 自动挂到 SqlSessionFactory 上。
     */
    @Bean
    public TsScopeGuard tsScopeGuard(StationScopeRegistry registry) {
        return new TsScopeGuard(registry);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 顺序要求：多租户必须在分页之前
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContextHolder.tenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return tableName == null || IGNORE_TABLES.contains(tableName.toLowerCase());
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
