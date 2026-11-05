package com.envmon.dao.scope;

/**
 * 站房归属快照：dataScope 判定所需的最小字段集。
 *
 * 只读不可变：时序查询守卫会把它缓存起来供每个请求复用，
 * 可变对象在并发下容易出现"读到一个正在被改的站房"。
 */
public final class StationScope {

    private final Long stationId;
    private final Long tenantId;
    private final Long opsOrgId;
    private final Long enterpriseId;
    private final Long regionId;
    private final Long opsEngineerId;

    public StationScope(Long stationId, Long tenantId, Long opsOrgId,
                        Long enterpriseId, Long regionId, Long opsEngineerId) {
        this.stationId = stationId;
        this.tenantId = tenantId;
        this.opsOrgId = opsOrgId;
        this.enterpriseId = enterpriseId;
        this.regionId = regionId;
        this.opsEngineerId = opsEngineerId;
    }

    public Long getStationId() {
        return stationId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getOpsOrgId() {
        return opsOrgId;
    }

    public Long getEnterpriseId() {
        return enterpriseId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public Long getOpsEngineerId() {
        return opsEngineerId;
    }
}
