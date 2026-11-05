package com.envmon.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;

/**
 * 租户上下文持有器。
 *
 * 用 TransmittableThreadLocal 而不是普通 ThreadLocal：
 * 异步任务、线程池、@Async 场景下普通 ThreadLocal 会丢值，
 * 导致租户条件失效——这是多租户系统最危险的 bug 之一。
 */
public final class TenantContextHolder {

    private static final TransmittableThreadLocal<TenantContext> HOLDER =
            new TransmittableThreadLocal<TenantContext>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext ctx) {
        HOLDER.set(ctx);
    }

    public static TenantContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 取租户；取不到直接抛错，宁可失败也不静默查全表 */
    public static Long tenantId() {
        TenantContext ctx = HOLDER.get();
        if (ctx == null || ctx.getTenantId() == null) {
            throw BizException.of(ErrorCode.TENANT_MISSING);
        }
        return ctx.getTenantId();
    }

    public static Long userId() {
        TenantContext ctx = HOLDER.get();
        return ctx == null || ctx.getUserId() == null ? 0L : ctx.getUserId();
    }

    public static String username() {
        TenantContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getUsername();
    }

    public static String traceId() {
        TenantContext ctx = HOLDER.get();
        return ctx == null || ctx.getTraceId() == null ? "-" : ctx.getTraceId();
    }

    public static boolean hasRole(String role) {
        TenantContext ctx = HOLDER.get();
        return ctx != null && ctx.getRoles() != null && ctx.getRoles().contains(role);
    }
}
