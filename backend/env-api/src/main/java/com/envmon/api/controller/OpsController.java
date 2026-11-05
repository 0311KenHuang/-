package com.envmon.api.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.envmon.common.api.PageResult;
import com.envmon.common.audit.Audit;
import com.envmon.dao.entity.OpsPlan;
import com.envmon.dao.entity.OpsPlanItem;
import com.envmon.dao.entity.OpsRecord;
import com.envmon.dao.entity.OpsTask;
import com.envmon.service.ops.OpsAuditService;
import com.envmon.service.ops.OpsPlanService;
import com.envmon.service.ops.OpsRecordService;
import com.envmon.service.ops.OpsRequest;
import com.envmon.service.ops.OpsTaskService;
import com.envmon.service.ops.OpsTraceService;
import com.envmon.service.ops.OpsView;
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
import java.util.List;
import java.util.Map;

/** 运维证据链：计划 → 任务 → 记录 → 审核，外加标记 ⇄ 工单核验 */
@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
public class OpsController {

    private final OpsPlanService planService;
    private final OpsTaskService taskService;
    private final OpsRecordService recordService;
    private final OpsAuditService auditService;
    private final OpsTraceService traceService;

    @PostMapping("/plan/generate")
    @SaCheckRole(value = {"SUP", "ORG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "ops", action = "PLAN_GENERATE")
    public Map<String, Object> generate(@Valid @RequestBody OpsRequest.PlanGenerate req) {
        return planService.generate(req);
    }

    @GetMapping("/plan")
    public List<OpsPlan> plans(@RequestParam(required = false) String period) {
        return planService.list(period);
    }

    @GetMapping("/plan/coverage")
    public OpsView.Coverage coverage(@RequestParam(required = false) String period) {
        return planService.coverage(period);
    }

    @GetMapping("/plan/{id}/items")
    public List<OpsPlanItem> planItems(@PathVariable Long id) {
        return planService.items(id);
    }

    @PostMapping("/plan/{id}/publish")
    @SaCheckRole(value = {"SUP", "ORG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "ops", action = "PLAN_PUBLISH")
    public Map<String, Object> publish(@PathVariable Long id) {
        return planService.publish(id);
    }

    @GetMapping("/task")
    public PageResult<OpsView.TaskView> tasks(OpsRequest.TaskQuery query) {
        return taskService.page(query);
    }

    @GetMapping("/task/{id}")
    public OpsTask task(@PathVariable Long id) {
        return taskService.detail(id);
    }

    @PatchMapping("/task/{id}/action")
    @SaCheckRole(value = {"SUP", "ORG", "MGR", "ENG"}, mode = SaMode.OR)
    @Audit(module = "ops", action = "TASK_ACTION")
    public OpsTask taskAction(@PathVariable Long id, @Valid @RequestBody OpsRequest.TaskAction req) {
        return taskService.act(id, req);
    }

    @PostMapping("/record")
    @SaCheckRole(value = {"SUP", "ORG", "MGR", "ENG"}, mode = SaMode.OR)
    @Audit(module = "ops", action = "RECORD_SUBMIT")
    public OpsRecord saveRecord(@Valid @RequestBody OpsRequest.RecordSubmit req) {
        return recordService.submit(req);
    }

    @PostMapping("/record/{id}/submit")
    @SaCheckRole(value = {"SUP", "ORG", "MGR", "ENG"}, mode = SaMode.OR)
    @Audit(module = "ops", action = "RECORD_SUBMIT_AUDIT")
    public OpsRecord submitRecord(@PathVariable Long id) {
        return recordService.submitForAudit(id);
    }

    @GetMapping("/record/pending")
    public List<OpsRecord> pendingRecords(@RequestParam(defaultValue = "20") int limit) {
        return recordService.pending(limit);
    }

    @GetMapping("/record/{id}")
    public OpsView.RecordDetail recordDetail(@PathVariable Long id) {
        return recordService.detail(id);
    }

    @PostMapping("/audit/{recordId}")
    @SaCheckRole(value = {"SUP", "ORG", "MGR", "REG"}, mode = SaMode.OR)
    @Audit(module = "ops", action = "RECORD_AUDIT")
    public OpsRecord review(@PathVariable Long recordId, @Valid @RequestBody OpsRequest.Audit req) {
        return auditService.review(recordId, req);
    }

    @GetMapping("/trace/check")
    public OpsView.TraceResult trace(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return traceService.check(from, to);
    }
}
