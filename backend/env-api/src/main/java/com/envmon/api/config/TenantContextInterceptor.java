package com.envmon.api.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 把 Sa-Token 会话里的租户/角色信息填进 TenantContext，
 * 后面的 Service 才能通过 TenantContextHolder.tenantId() 走多租户插件。
 *
 * 顺序必须排在 Sa-Token 鉴权拦截器之后，否则拿不到登录态。
 */
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!StpUtil.isLogin()) {
            return true;
        }
        SaSession session = StpUtil.getSession(false);
        if (session == null) {
            return true;
        }
        TenantContext ctx = TenantContextHolder.get();
        if (ctx == null) {
            ctx = new TenantContext();
            TenantContextHolder.set(ctx);
        }
        Object tenantId = session.get("tenantId");
        if (tenantId != null) {
            ctx.setTenantId(Long.valueOf(String.valueOf(tenantId)));
        }
        Object roles = session.get("roles");
        if (roles instanceof List) {
            ctx.setRoles((List<String>) roles);
        }
        Object scope = session.get("dataScope");
        if (scope != null) {
            ctx.setDataScope(String.valueOf(scope));
        }
        Object username = session.get("username");
        if (username != null) {
            ctx.setUsername(String.valueOf(username));
        }
        // orgId/regionId 必须每次请求从会话恢复，
        // 否则 ORG/REGION 数据权限在"非登录请求"里拿不到归属条件
        Object orgId = session.get("orgId");
        if (orgId != null) {
            ctx.setOrgId(Long.valueOf(String.valueOf(orgId)));
        }
        Object regionId = session.get("regionId");
        if (regionId != null) {
            ctx.setRegionId(Long.valueOf(String.valueOf(regionId)));
        }
        if (ctx.getUserId() == null) {
            ctx.setUserId(Long.valueOf(String.valueOf(StpUtil.getLoginId())));
        }
        return true;
    }
}
