package com.envmon.common.context;

/**
 * 数据权限（dataScope）统一判定规则。
 *
 * 放在 env-common 是因为 env-service 和 env-ts 都要用：
 * - env-service：列表查询（站房/任务/报警）按作用域过滤
 * - env-ts：ts_* 表不受租户插件保护，requireStation 入口用它做单站房断言
 *
 * 语义（与 sys_role.data_scope 对应）：
 *   ALL    —— 不限制
 *   REGION —— 只能看自己 regionId 下的站房；未绑定 region 的账号视为未配置，不限制
 *             （与历史行为兼容，绑定后立即收紧；见 V10__user_region.sql）
 *   ORG    —— 只能看自己 org 承运（ops_org_id）或所属企业（enterprise_id）的站房
 *   SELF   —— 只能看自己负责（ops_engineer_id）的站房
 * 无 org/region 且作用域非 ALL 时默认拒绝（返回 false），宁严勿漏。
 */
public final class DataScopeRules {

    private DataScopeRules() {
    }

    /**
     * 判断给定归属信息的站房对当前上下文是否可见。
     *
     * @param scope          数据权限（null 按 SELF 处理）
     * @param orgId          用户 org（运维单位或企业）
     * @param regionId       用户区域（REGION 用，可空）
     * @param userId         用户 id（SELF 用）
     * @param stationOpsOrgId    站房承运运维单位
     * @param stationEnterpriseId 站房所属企业
     * @param stationRegionId     站房区域
     * @param stationOpsEngineerId 站房责任工程师
     */
    public static boolean allows(TenantContext ctx,
                                 Long stationOpsOrgId,
                                 Long stationEnterpriseId,
                                 Long stationRegionId,
                                 Long stationOpsEngineerId) {
        if (ctx == null) {
            return false;
        }
        String scope = ctx.getDataScope() == null ? "SELF" : ctx.getDataScope();
        switch (scope) {
            case "ALL":
                return true;
            case "REGION":
                return ctx.getRegionId() == null
                        || ctx.getRegionId().equals(stationRegionId);
            case "ORG":
                if (ctx.getOrgId() == null) {
                    return false;
                }
                return ctx.getOrgId().equals(stationOpsOrgId)
                        || ctx.getOrgId().equals(stationEnterpriseId);
            case "SELF":
            default:
                return ctx.getUserId() != null
                        && ctx.getUserId().equals(stationOpsEngineerId);
        }
    }

    /** 便捷重载：直接传上下文字段值（env-ts 不依赖实体时用） */
    public static boolean allowsScope(String scope, Long orgId, Long regionId, Long userId,
                                      Long stationOpsOrgId, Long stationEnterpriseId,
                                      Long stationRegionId, Long stationOpsEngineerId) {
        TenantContext ctx = new TenantContext();
        ctx.setDataScope(scope);
        ctx.setOrgId(orgId);
        ctx.setRegionId(regionId);
        ctx.setUserId(userId);
        return allows(ctx, stationOpsOrgId, stationEnterpriseId, stationRegionId, stationOpsEngineerId);
    }
}
