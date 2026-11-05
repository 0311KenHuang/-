package com.envmon.service.datascope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.mapper.MdStationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据权限（dataScope）落地服务。
 *
 * 此前 dataScope 只存进会话，业务查询从不消费——ORG/SELF 用户能看到租户内全部数据。
 * 这里把它变成真正可执行的过滤条件，核心抽象是"可见站房集合"：
 *
 *   visibleStationIds() 返回 null  = 不限制（ALL / 未绑定区域的 REGION）
 *   返回集合（可能为空）            = 只允许这些站房，空集即全部拒绝
 *
 * 用法约定：
 * - 列表查询：非 null 时 wrapper.in(station_id, ids)
 * - 单对象查询：先取实体再 assertVisible，避免先查集合再判断的两次开销
 *
 * 站房之外的业务（任务/报警/记录）都以 station_id 为主键关联，复用同一集合即可。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final MdStationMapper stationMapper;

    /**
     * 当前用户可见站房 id 集合。
     *
     * @return null 表示不限制；空集合表示无权查看任何站房
     */
    public List<Long> visibleStationIds() {
        TenantContext ctx = TenantContextHolder.get();
        String scope = ctx == null || ctx.getDataScope() == null ? "SELF" : ctx.getDataScope();
        if ("ALL".equals(scope)) {
            return null;
        }
        if ("REGION".equals(scope)) {
            if (ctx == null || ctx.getRegionId() == null) {
                // 未绑定区域的监管账号：维持租户内全量（升级兼容，见 V10）
                log.debug("REGION 作用域但用户未绑定区域，暂不限制 tenantId={}",
                        ctx == null ? null : ctx.getTenantId());
                return null;
            }
            List<MdStation> list = stationMapper.selectList(new LambdaQueryWrapper<MdStation>()
                    .eq(MdStation::getRegionId, ctx.getRegionId())
                    .select(MdStation::getId));
            return toIds(list);
        }
        if ("ORG".equals(scope)) {
            if (ctx == null || ctx.getOrgId() == null) {
                return Collections.emptyList();
            }
            // orgId 可能是承运运维单位（ops_org_id）也可能是企业（enterprise_id），
            // 两者都在租户内，OR 匹配不会造成跨组织泄露
            List<MdStation> list = stationMapper.selectList(new LambdaQueryWrapper<MdStation>()
                    .and(w -> w.eq(MdStation::getOpsOrgId, ctx.getOrgId())
                            .or()
                            .eq(MdStation::getEnterpriseId, ctx.getOrgId()))
                    .select(MdStation::getId));
            return toIds(list);
        }
        // SELF
        if (ctx == null || ctx.getUserId() == null) {
            return Collections.emptyList();
        }
        List<MdStation> list = stationMapper.selectList(new LambdaQueryWrapper<MdStation>()
                .eq(MdStation::getOpsEngineerId, ctx.getUserId())
                .select(MdStation::getId));
        return toIds(list);
    }

    /** 单站房可见性断言，实体已带租户隔离（插件兜底），这里只补作用域 */
    public void assertVisible(MdStation station) {
        TenantContext ctx = TenantContextHolder.get();
        if (!com.envmon.common.context.DataScopeRules.allows(ctx,
                station.getOpsOrgId(), station.getEnterpriseId(),
                station.getRegionId(), station.getOpsEngineerId())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权访问该站房");
        }
    }

    /** 把可见性断言用于 id（内部自行查询站房） */
    public void assertStationVisible(Long stationId) {
        MdStation station = stationId == null ? null : stationMapper.selectById(stationId);
        if (station == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "站房不存在或无权限");
        }
        assertVisible(station);
    }

    private List<Long> toIds(List<MdStation> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<Long>(list.size());
        for (MdStation s : list) {
            ids.add(s.getId());
        }
        return ids;
    }
}
