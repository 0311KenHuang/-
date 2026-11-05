package com.envmon.service.ops;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.AlmAction;
import com.envmon.dao.entity.AlmEvent;
import com.envmon.dao.entity.OpsAudit;
import com.envmon.dao.entity.OpsRecord;
import com.envmon.dao.mapper.AlmActionMapper;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.dao.mapper.OpsAuditMapper;
import com.envmon.dao.mapper.OpsRecordMapper;
import com.envmon.service.notify.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 记录审核：审核人不得为执行人，驳回必填原因，通过后闭环任务与关联报警 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpsAuditService {

    private final OpsRecordService recordService;
    private final OpsRecordMapper recordMapper;
    private final OpsAuditMapper auditMapper;
    private final OpsTaskService taskService;
    private final AlmEventMapper eventMapper;
    private final AlmActionMapper actionMapper;
    private final NotificationService notificationService;

    @Transactional(rollbackFor = Exception.class)
    public OpsRecord review(Long recordId, OpsRequest.Audit req) {
        OpsRecord record = recordService.require(recordId);
        if (!"SUBMITTED".equals(record.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "只有已提交的记录可以审核");
        }
        String result = req.getResult() == null ? "" : req.getResult().trim().toUpperCase();
        if (!"PASS".equals(result) && !"REJECT".equals(result)) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "审核结果只能是 PASS 或 REJECT");
        }
        Long auditorId = TenantContextHolder.userId();
        OpsStateMachine.assertAuditorAllowed(record.getEngineerId(), auditorId);
        if ("REJECT".equals(result) && StringUtils.isBlank(req.getReason())) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "驳回必须填写原因");
        }

        OpsAudit audit = new OpsAudit();
        audit.setTenantId(record.getTenantId());
        audit.setRecordId(recordId);
        audit.setAuditorId(auditorId == null ? 0L : auditorId);
        audit.setResult(result);
        audit.setReason(req.getReason());
        auditMapper.insert(audit);

        if ("PASS".equals(result)) {
            record.setStatus("APPROVED");
            record.setSignAuditor(TenantContextHolder.username());
            if (record.getEndTime() == null) {
                record.setEndTime(LocalDateTime.now());
            }
            recordMapper.updateById(record);
            taskService.closeByAudit(record.getTaskId(), auditorId);
            closeLinkedAlarm(record.getTaskId(), auditorId, record.getTenantId());
        } else {
            record.setStatus("REJECTED");
            recordMapper.updateById(record);
            int rejects = auditMapper.countRejects(recordId);
            if (rejects >= 3) {
                log.warn("BR-04：记录 {} 已被驳回 {} 次，需升级至运维单位管理员介入", recordId, rejects);
            }
        }
        return record;
    }

    /** 审核通过即关闭由该任务承接的报警，形成报警→工单→记录→审核闭环 */
    private void closeLinkedAlarm(Long taskId, Long operator, Long tenantId) {
        List<AlmEvent> events = eventMapper.selectList(new QueryWrapper<AlmEvent>()
                .eq("related_task_id", taskId)
                .in("status", "OPEN", "ACK", "PROCESSING"));
        for (AlmEvent event : events) {
            event.setStatus("CLOSED");
            event.setEndAt(LocalDateTime.now());
            eventMapper.updateById(event);

            AlmAction action = new AlmAction();
            action.setTenantId(tenantId);
            action.setEventId(event.getId());
            action.setAction("AUTO_CLOSE");
            action.setOperator(operator == null ? 0L : operator);
            action.setRemark("关联运维记录审核通过，自动关闭");
            actionMapper.insert(action);
            notificationService.onRecovered(event);
        }
    }
}
