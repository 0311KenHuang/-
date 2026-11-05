package com.envmon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envmon.common.api.PageResult;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.entity.MdStationFactor;
import com.envmon.dao.entity.MdStationStop;
import com.envmon.dao.mapper.MdStationFactorMapper;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.dao.mapper.MdStationStopMapper;
import com.envmon.service.datascope.DataScopeService;
import com.envmon.service.dto.StationQuery;
import com.envmon.service.dto.StationSaveRequest;
import com.envmon.ts.service.TsAggregateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {

    private final MdStationMapper stationMapper;
    private final MdStationFactorMapper factorMapper;
    private final MdStationStopMapper stopMapper;
    private final TsAggregateService tsAggregateService;
    private final DataScopeService dataScopeService;

    /** 分页查询：租户隔离与软删除由拦截器自动处理，这里只写业务条件 */
    public PageResult<MdStation> page(StationQuery q) {
        LambdaQueryWrapper<MdStation> w = new LambdaQueryWrapper<MdStation>();
        // dataScope 落地：ORG/SELF 用户只能看到自己组织/负责的站房（null = ALL 不限制）
        List<Long> visible = dataScopeService.visibleStationIds();
        if (visible != null) {
            if (visible.isEmpty()) {
                return PageResult.of(java.util.Collections.emptyList(), 0, q.getPage(), q.getPageSize());
            }
            w.in(MdStation::getId, visible);
        }
        if (q.getEnterpriseId() != null) {
            w.eq(MdStation::getEnterpriseId, q.getEnterpriseId());
        }
        if (q.getRegionId() != null) {
            w.eq(MdStation::getRegionId, q.getRegionId());
        }
        if (StringUtils.isNotBlank(q.getStationType())) {
            w.eq(MdStation::getStationType, q.getStationType());
        }
        if (StringUtils.isNotBlank(q.getBizStatus())) {
            w.eq(MdStation::getBizStatus, q.getBizStatus());
        }
        if (StringUtils.isNotBlank(q.getKeyword())) {
            final String kw = q.getKeyword().trim();
            w.and(x -> x.like(MdStation::getName, kw).or().like(MdStation::getStationCode, kw));
        }
        w.orderByDesc(MdStation::getId);

        Page<MdStation> page = stationMapper.selectPage(new Page<MdStation>(q.getPage(), q.getPageSize()), w);
        return PageResult.of(page.getRecords(), page.getTotal(), q.getPage(), q.getPageSize());
    }

    public MdStation detail(Long id) {
        MdStation s = stationMapper.selectById(id);
        if (s == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "站房不存在");
        }
        // dataScope 落地：单站房读取同样受作用域约束（update/changeStatus/stops/factors 都从这里过）
        dataScopeService.assertVisible(s);
        return s;
    }

    public MdStation create(StationSaveRequest req) {
        MdStation s = new MdStation();
        // 显式带租户：租户插件会在 SQL 里补，但实体上没有值，返回给前端会是 null
        s.setTenantId(com.envmon.common.context.TenantContextHolder.tenantId());
        s.setStationCode(req.getStationCode());
        s.setName(req.getName());
        s.setStationType(req.getStationType());
        s.setEnterpriseId(req.getEnterpriseId());
        s.setRegionId(req.getRegionId());
        s.setLng(req.getLng());
        s.setLat(req.getLat());
        s.setAddress(req.getAddress());
        s.setOpsOrgId(req.getOpsOrgId());
        s.setOpsEngineerId(req.getOpsEngineerId());
        s.setBizStatus("NORMAL");
        s.setIsFocus(0);
        s.setDeleted(0);
        stationMapper.insert(s);
        return s;
    }

    public MdStation update(Long id, StationSaveRequest req) {
        MdStation s = detail(id);
        s.setStationCode(req.getStationCode());
        s.setName(req.getName());
        s.setStationType(req.getStationType());
        s.setEnterpriseId(req.getEnterpriseId());
        s.setRegionId(req.getRegionId());
        s.setLng(req.getLng());
        s.setLat(req.getLat());
        s.setAddress(req.getAddress());
        s.setOpsOrgId(req.getOpsOrgId());
        s.setOpsEngineerId(req.getOpsEngineerId());
        stationMapper.updateById(s);
        return s;
    }

    /**
     * 停运必须填原因——监管场景下这是审计要点。
     *
     * 同时维护停运区间：切到 STOPPED 开区间，切出 STOPPED 关区间。
     * 区间是附录 G 剔除统计的依据，不能只记一个当前状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public MdStation changeStatus(Long id, String bizStatus, String stopReason) {
        MdStation s = detail(id);
        boolean wasStopped = "STOPPED".equals(s.getBizStatus());
        boolean nowStopped = "STOPPED".equals(bizStatus);

        s.setBizStatus(bizStatus);
        s.setStopReason(nowStopped ? stopReason : null);
        stationMapper.updateById(s);

        LocalDateTime now = LocalDateTime.now();
        if (nowStopped && !wasStopped) {
            // 残缺的开放区间先收口，避免同一时间有两个 end_at 为空
            stopMapper.closeOpen(id, now);
            MdStationStop stop = new MdStationStop();
            stop.setTenantId(TenantContextHolder.tenantId());
            stop.setStationId(id);
            stop.setBeginAt(now);
            stop.setReason(stopReason);
            stop.setSource("STATUS");
            stopMapper.insert(stop);
        } else if (!nowStopped && wasStopped) {
            MdStationStop open = stopMapper.selectOpen(id);
            LocalDateTime stopBegin = open == null || open.getBeginAt() == null
                    ? now.minusDays(7) : open.getBeginAt();
            int closed = stopMapper.closeOpen(id, now);
            if (closed > 0) {
                // 从停运开始重算到现在：停运期间的数据不再参与统计，小时值要跟着变。
                // 聚合内部按 500 行分片 upsert，长期停运也不会撑爆单条 SQL。
                tsAggregateService.aggregateStationHours(id,
                        stopBegin.truncatedTo(ChronoUnit.HOURS),
                        now.truncatedTo(ChronoUnit.HOURS).plusHours(1));
            }
        }
        return s;
    }

    /** 停运区间（默认最近 30 天），供页面展示与核对统计口径 */
    public List<MdStationStop> stops(Long stationId, LocalDate from, LocalDate to) {
        detail(stationId);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate begin = from == null ? end.minusDays(30) : from;
        return stopMapper.selectOverlap(stationId, begin.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    /** 站房因子档案：实时值页面要显示单位与限值 */
    public List<MdStationFactor> factors(Long stationId) {
        detail(stationId);
        return factorMapper.selectList(new QueryWrapper<MdStationFactor>()
                .eq("station_id", stationId)
                .orderByAsc("sort"));
    }

    public List<MdStation> listByEnterprise(Long enterpriseId) {
        List<MdStation> list = stationMapper.selectList(new LambdaQueryWrapper<MdStation>()
                .eq(MdStation::getEnterpriseId, enterpriseId));
        // dataScope 落地：按作用域过滤后返回
        List<Long> visible = dataScopeService.visibleStationIds();
        if (visible == null) {
            return list;
        }
        java.util.Set<Long> allow = new java.util.HashSet<Long>(visible);
        List<MdStation> out = new java.util.ArrayList<MdStation>();
        for (MdStation s : list) {
            if (allow.contains(s.getId())) {
                out.add(s);
            }
        }
        return out;
    }
}
