package com.envmon.api.controller;

import com.envmon.dao.entity.DevParamChange;
import com.envmon.dao.entity.DevParamSnapshot;
import com.envmon.service.ops.DeviceParamQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/device/params")
@RequiredArgsConstructor
public class DeviceParamController {

    private final DeviceParamQueryService queryService;

    /** 某站房各设备各参数的最新值 */
    @GetMapping("/latest")
    public List<DevParamSnapshot> latest(@RequestParam Long stationId) {
        return queryService.latest(stationId);
    }

    /** 参数历史快照 */
    @GetMapping
    public List<DevParamSnapshot> history(
            @RequestParam Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "200") int limit) {
        return queryService.history(stationId, from, to, limit);
    }

    /** 参数变更留痕（含关联报警事件） */
    @GetMapping("/changes")
    public List<DevParamChange> changes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "100") int limit) {
        return queryService.changes(from, to, limit);
    }
}
