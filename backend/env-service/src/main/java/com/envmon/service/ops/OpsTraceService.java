package com.envmon.service.ops;

import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.dto.FlaggedRow;
import com.envmon.dao.entity.OpsRecord;
import com.envmon.dao.mapper.OpsRecordMapper;
import com.envmon.dao.mapper.TsDataMinuteMapper;
import com.envmon.service.datascope.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 标记 ⇄ 工单双向核验（docs/01 §4 核心差异化能力）。
 *
 * BR-05 正向：数据标记 C/M/D/Td 的时段，必须找到时间重叠的已审核运维记录，
 *             找不到就是「疑似虚假标记」。
 * BR-06 反向：有已审核的校准记录，但校准前后各 30 分钟窗口内没有 C 标记，
 *             判为「漏标」。
 *
 * 核验横跨全部站房，但结果只应呈现当前用户可见的部分：
 * 先按 dataScope 过滤一批，再进时序查询——否则既会泄露别的组织有哪些站房，
 * 也会让时序守卫在遇到越权站房时抛错，整页核验直接失败。
 */
@Service
@RequiredArgsConstructor
public class OpsTraceService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final TsDataMinuteMapper minuteMapper;
    private final OpsRecordMapper recordMapper;
    private final DataScopeService dataScopeService;

    public OpsView.TraceResult check(LocalDate from, LocalDate to) {
        LocalDate begin = from == null ? LocalDate.now().minusDays(1) : from;
        LocalDate end = to == null ? LocalDate.now() : (to.isBefore(begin) ? begin : to);
        LocalDateTime fromTs = begin.atStartOfDay();
        LocalDateTime toTs = end.plusDays(1).atStartOfDay();
        Long tenantId = TenantContextHolder.tenantId();

        // null = 不限制（ALL）；否则只保留可见站房
        List<Long> visibleIds = dataScopeService.visibleStationIds();
        Set<Long> visible = visibleIds == null ? null : new HashSet<Long>(visibleIds);

        OpsView.TraceResult result = new OpsView.TraceResult();
        List<FlaggedRow> flagged = minuteMapper.selectFlagged(tenantId, fromTs, toTs);
        if (flagged != null) {
            for (FlaggedRow row : flagged) {
                if (visible != null && !visible.contains(row.getStationId())) {
                    continue;
                }
                List<OpsRecord> records = recordMapper.selectApprovedOverlap(
                        tenantId, row.getStationId(), row.getMinTs(), row.getMaxTs().plusMinutes(1));
                if (records == null || records.isEmpty()) {
                    result.getFakeMarks().add("站房 " + row.getStationId() + " 标记 " + row.getDevFlag()
                            + "（" + time(row.getMinTs()) + " ~ " + time(row.getMaxTs())
                            + "）无对应已审核运维记录");
                }
            }
        }

        List<OpsRecord> calibrations = recordMapper.selectApprovedCalibration(tenantId, fromTs, toTs);
        if (calibrations != null) {
            for (OpsRecord record : calibrations) {
                if (visible != null && !visible.contains(record.getStationId())) {
                    continue;
                }
                LocalDateTime markStart = record.getMarkBegin() == null
                        ? record.getBeginTime() : record.getMarkBegin();
                LocalDateTime markEnd = record.getMarkEnd() != null ? record.getMarkEnd()
                        : record.getEndTime() != null ? record.getEndTime() : markStart.plusHours(1);
                if (markStart == null) {
                    continue;
                }
                int count = minuteMapper.countFlagged(record.getStationId(),
                        markStart.minusMinutes(30), markEnd.plusMinutes(30), "C");
                if (count == 0) {
                    result.getMissingMarks().add("记录 " + record.getRecordNo() + "（站房 "
                            + record.getStationId() + " 校准）对应时段无 C 标记，疑似漏标");
                }
            }
        }
        return result;
    }

    private String time(LocalDateTime t) {
        return t == null ? "-" : TIME.format(t);
    }
}
