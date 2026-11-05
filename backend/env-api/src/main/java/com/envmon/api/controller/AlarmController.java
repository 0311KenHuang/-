package com.envmon.api.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.envmon.common.api.PageResult;
import com.envmon.common.audit.Audit;
import com.envmon.service.alarm.AlarmEngine;
import com.envmon.service.alarm.AlarmEventService;
import com.envmon.service.alarm.dto.AlarmEventQuery;
import com.envmon.service.alarm.dto.AlarmEventView;
import com.envmon.service.alarm.dto.AlarmHandleRequest;
import com.envmon.dao.entity.OpsTask;
import com.envmon.service.ops.OpsTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;

/** 报警一键转工单的可选入参（原裸 Map<String,Object> 的类型化替代） */
class AlarmTaskRequest {
    /** 改派目标（可空，Service 默认规则指派） */
    private Long assigneeId;
    /** 工单截止时间（可空） */
    private LocalDateTime deadline;

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
}

@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmEventService eventService;
    private final AlarmEngine alarmEngine;
    private final OpsTaskService opsTaskService;

    /** 报警事件分页：工作台待办与报警列表共用 */
    @GetMapping("/events")
    public PageResult<AlarmEventView> page(AlarmEventQuery query) {
        return eventService.page(query);
    }

    /** 认领 / 关闭 / 标记误报，全部写 alm_action 留痕 */
    @PatchMapping("/events/{id}/handle")
    @SaCheckRole(value = {"SUP", "REG", "ORG", "MGR", "ENG"}, mode = SaMode.OR)
    @Audit(module = "alarm", action = "HANDLE")
    public AlarmEventView handle(@PathVariable Long id, @Valid @RequestBody AlarmHandleRequest req) {
        return eventService.handle(id, req.getAction(), req.getRemark());
    }

    /** 报警一键转工单：一个报警只允许生成一张任务 */
    @PostMapping("/events/{id}/task")
    @SaCheckRole(value = {"SUP", "REG", "ORG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "alarm", action = "CREATE_TASK")
    public OpsTask createTask(@PathVariable Long id,
                              @Valid @RequestBody(required = false) AlarmTaskRequest req) {
        return opsTaskService.createFromAlarm(id,
                req == null ? null : req.getAssigneeId(),
                req == null ? null : req.getDeadline());
    }

    /** 手工触发扫描：规则调整后立即验证，生产主要靠定时任务 */
    @PostMapping("/scan")
    @SaCheckRole(value = {"SUP", "REG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "alarm", action = "SCAN")
    public Map<String, Object> scan() {
        return alarmEngine.scanAllTenants();
    }
}
