package com.envmon.service.ops;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.OpsRecord;
import com.envmon.dao.entity.OpsRecordItem;
import com.envmon.dao.entity.OpsRecordMedia;
import com.envmon.dao.entity.OpsTask;
import com.envmon.dao.mapper.OpsAuditMapper;
import com.envmon.dao.mapper.OpsRecordItemMapper;
import com.envmon.dao.mapper.OpsRecordMediaMapper;
import com.envmon.dao.mapper.OpsRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** 现场记录：一个任务一份，明细/照片/标记区间构成证据链主体 */
@Service
@RequiredArgsConstructor
public class OpsRecordService {

    private final OpsRecordMapper recordMapper;
    private final OpsRecordItemMapper itemMapper;
    private final OpsRecordMediaMapper mediaMapper;
    private final OpsAuditMapper auditMapper;
    private final OpsTaskService taskService;
    private final OpsNoGenerator noGenerator;

    /** 填报/修改记录。审核通过后不可再改；驳回后允许修订重提。 */
    @Transactional(rollbackFor = Exception.class)
    public OpsRecord submit(OpsRequest.RecordSubmit req) {
        OpsTask task = taskService.detail(req.getTaskId());
        if (!Arrays.asList("ARRIVED", "SUBMITTED").contains(task.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "任务未到场或已结束，不能填报记录");
        }
        Long tenantId = TenantContextHolder.tenantId();
        Long userId = TenantContextHolder.userId();

        OpsRecord record = recordMapper.selectOne(new QueryWrapper<OpsRecord>()
                .eq("task_id", task.getId()).last("LIMIT 1"));
        if (record != null && "APPROVED".equals(record.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "记录已审核通过，不可修改");
        }
        if (record == null) {
            record = new OpsRecord();
            record.setTenantId(tenantId);
            record.setRecordNo(noGenerator.next("RC"));
            record.setTaskId(task.getId());
            record.setStationId(task.getStationId());
            record.setOpsType(task.getOpsType());
            record.setEngineerId(userId);
            record.setDeleted(0);
        }
        record.setBeginTime(req.getBeginTime() == null ? LocalDateTime.now() : req.getBeginTime());
        record.setEndTime(req.getEndTime());
        record.setMarkBegin(req.getMarkBegin());
        record.setMarkEnd(req.getMarkEnd());
        record.setConclusion(req.getConclusion());
        record.setNextPlanHint(req.getNextPlanHint());
        record.setStatus("DRAFT");
        record.setSignEngineer(TenantContextHolder.username());
        if (record.getId() == null) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
            itemMapper.delete(new QueryWrapper<OpsRecordItem>().eq("record_id", record.getId()));
            mediaMapper.delete(new QueryWrapper<OpsRecordMedia>().eq("record_id", record.getId()));
        }

        if (req.getItems() != null) {
            for (OpsRequest.Item src : req.getItems()) {
                OpsRecordItem item = new OpsRecordItem();
                item.setTenantId(tenantId);
                item.setRecordId(record.getId());
                item.setItemCode(src.getItemCode());
                item.setItemName(src.getItemName());
                item.setValueType(src.getValueType());
                item.setValue(src.getValue());
                item.setUnit(src.getUnit());
                item.setNormalFlag(src.getNormalFlag());
                itemMapper.insert(item);
            }
        }
        if (req.getMedia() != null) {
            for (OpsRequest.Media src : req.getMedia()) {
                OpsRecordMedia media = new OpsRecordMedia();
                media.setTenantId(tenantId);
                media.setRecordId(record.getId());
                media.setMediaType(src.getMediaType() == null ? "PHOTO" : src.getMediaType());
                media.setObjectKey(src.getObjectKey());
                media.setWatermarkJson(src.getWatermarkJson());
                media.setSha256(src.getSha256());
                mediaMapper.insert(media);
            }
        }
        return record;
    }

    /** 提交审核：同时把任务推进到 SUBMITTED */
    @Transactional(rollbackFor = Exception.class)
    public OpsRecord submitForAudit(Long recordId) {
        OpsRecord record = require(recordId);
        if ("APPROVED".equals(record.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "记录已审核通过");
        }
        if (itemMapper.selectCount(new QueryWrapper<OpsRecordItem>().eq("record_id", record.getId())) == 0) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "记录没有检查项，不能提交审核");
        }
        OpsTask task = taskService.detail(record.getTaskId());
        if ("ARRIVED".equals(task.getStatus())) {
            OpsRequest.TaskAction action = new OpsRequest.TaskAction();
            action.setAction("SUBMIT");
            action.setRemark("运维记录提交审核");
            taskService.act(task.getId(), action);
        } else if (!"SUBMITTED".equals(task.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "任务当前状态不允许提交审核");
        }
        record.setStatus("SUBMITTED");
        recordMapper.updateById(record);
        return record;
    }

    public OpsView.RecordDetail detail(Long recordId) {
        OpsRecord record = require(recordId);
        OpsView.RecordDetail view = new OpsView.RecordDetail();
        view.setRecord(record);
        view.setTask(taskService.detail(record.getTaskId()));
        view.setItems(itemMapper.selectList(new QueryWrapper<OpsRecordItem>().eq("record_id", recordId)));
        view.setMedia(mediaMapper.selectList(new QueryWrapper<OpsRecordMedia>().eq("record_id", recordId)));
        List<com.envmon.dao.entity.OpsAudit> audits = auditMapper.selectList(
                new QueryWrapper<com.envmon.dao.entity.OpsAudit>().eq("record_id", recordId));
        view.setAudits(audits);
        return view;
    }

    public List<OpsRecord> pending(int limit) {
        return recordMapper.selectList(new QueryWrapper<OpsRecord>()
                .eq("status", "SUBMITTED")
                .orderByAsc("begin_time")
                .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
    }

    OpsRecord require(Long id) {
        OpsRecord record = id == null ? null : recordMapper.selectById(id);
        if (record == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "运维记录不存在");
        }
        return record;
    }
}
