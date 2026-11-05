package com.envmon.service.ops;

import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.entity.DevParamChange;
import com.envmon.dao.entity.DevParamSnapshot;
import com.envmon.dao.mapper.DevParamChangeMapper;
import com.envmon.dao.mapper.DevParamSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 工作参数查询：最新值、历史快照、变更留痕 */
@Service
@RequiredArgsConstructor
public class DeviceParamQueryService {

    private final DevParamSnapshotMapper snapshotMapper;
    private final DevParamChangeMapper changeMapper;

    public List<DevParamSnapshot> latest(Long stationId) {
        return snapshotMapper.selectLatest(TenantContextHolder.tenantId(), stationId);
    }

    public List<DevParamSnapshot> history(Long stationId, LocalDate from, LocalDate to, int limit) {
        LocalDate begin = from == null ? LocalDate.now().minusDays(7) : from;
        // to 缺省必须到"今天"，否则窗口只有 begin 当天一天，历史数据会被漏掉
        LocalDate end = to == null ? LocalDate.now() : (to.isBefore(begin) ? begin : to);
        return snapshotMapper.selectByStation(TenantContextHolder.tenantId(), stationId,
                begin.atStartOfDay(), end.plusDays(1).atStartOfDay(),
                limit <= 0 ? 200 : Math.min(limit, 1000));
    }

    public List<DevParamChange> changes(LocalDate from, LocalDate to, int limit) {
        LocalDate begin = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now() : (to.isBefore(begin) ? begin : to);
        return changeMapper.selectRecent(TenantContextHolder.tenantId(),
                begin.atStartOfDay(), end.plusDays(1).atStartOfDay(),
                limit <= 0 ? 100 : Math.min(limit, 500));
    }
}
