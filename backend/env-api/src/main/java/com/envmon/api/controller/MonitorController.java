package com.envmon.api.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.envmon.common.audit.Audit;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.entity.TsDataDay;
import com.envmon.service.MonitorService;
import com.envmon.ts.dto.StationRealtime;
import com.envmon.ts.dto.ValidRateResult;
import com.envmon.ts.service.TsBackfillService;
import com.envmon.ts.service.TsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;
    private final TsQueryService tsQueryService;
    private final TsBackfillService tsBackfillService;

    /** 工作台概览 */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return monitorService.overview();
    }

    /** 工作台"站房实时值"：跨站房取各因子最新一条 */
    @GetMapping("/realtime")
    public List<StationRealtime> realtime(@RequestParam(defaultValue = "200") int limit) {
        return tsQueryService.realtime(limit);
    }

    /** 单站房实时值：已配置但未上报的因子也会返回，便于前端显示缺数 */
    @GetMapping("/station/{stationId}/realtime")
    public StationRealtime stationRealtime(@PathVariable Long stationId) {
        return tsQueryService.stationRealtime(stationId);
    }

    /** 单站房某天有效率（附录 G） */
    @GetMapping("/station/{stationId}/valid-rate")
    public ValidRateResult validRate(
            @PathVariable Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tsQueryService.validRate(stationId, date);
    }

    /** 单站房某天已生成的日数据（附录 G） */
    @GetMapping("/station/{stationId}/day")
    public List<TsDataDay> dayData(
            @PathVariable Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tsQueryService.dayData(stationId, date);
    }

    /**
     * 历史报文重算：补解析 + 重算小时/日。
     * 只在"解析功能上线前已有报文"或"某天聚合算错"时使用，幂等。
     */
    @PostMapping("/ts/rebuild")
    @SaCheckRole(value = {"SUP", "REG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "monitor", action = "REBUILD")
    public Map<String, Object> rebuild(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return tsBackfillService.rebuild(TenantContextHolder.tenantId(), from, to);
    }
}
