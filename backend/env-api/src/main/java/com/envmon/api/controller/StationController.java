package com.envmon.api.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.envmon.common.api.PageResult;
import com.envmon.common.audit.Audit;
import com.envmon.dao.entity.MdStation;
import com.envmon.service.StationService;
import com.envmon.service.dto.StationChangeStatusRequest;
import com.envmon.service.dto.StationQuery;
import com.envmon.service.dto.StationSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/station")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping
    public PageResult<MdStation> page(StationQuery query) {
        return stationService.page(query);
    }

    @GetMapping("/{id}")
    public MdStation detail(@PathVariable Long id) {
        return stationService.detail(id);
    }

    @PostMapping
    @SaCheckRole(value = {"ORG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "station", action = "CREATE")
    public MdStation create(@Valid @RequestBody StationSaveRequest req) {
        return stationService.create(req);
    }

    @PatchMapping("/{id}")
    @SaCheckRole(value = {"ORG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "station", action = "UPDATE")
    public MdStation update(@PathVariable Long id, @Valid @RequestBody StationSaveRequest req) {
        return stationService.update(id, req);
    }

    @PatchMapping("/{id}/status")
    @SaCheckRole(value = {"ORG", "MGR", "REG"}, mode = SaMode.OR)
    @Audit(module = "station", action = "CHANGE_STATUS")
    public MdStation changeStatus(@PathVariable Long id,
                                  @Valid @RequestBody StationChangeStatusRequest req) {
        return stationService.changeStatus(id, req.getBizStatus(), req.getStopReason());
    }

    @GetMapping("/{id}/factors")
    public Map<String, Object> factors(@PathVariable Long id) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("stationId", id);
        m.put("list", stationService.factors(id));
        return m;
    }

    /** 停运区间：附录 G 统计剔除依据，默认最近 30 天 */
    @GetMapping("/{id}/stops")
    public Map<String, Object> stops(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("stationId", id);
        m.put("list", stationService.stops(id, from, to));
        return m;
    }
}
