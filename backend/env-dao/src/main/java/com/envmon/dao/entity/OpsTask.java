package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 运维任务：SLA 三个时间点独立留痕（接单 / 到场 / 完成） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ops_task")
public class OpsTask extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private String taskNo;
    /** 来源计划明细；报警/故障类任务为空 */
    private Long planItemId;
    private Long stationId;
    private String opsType;
    /** PLAN / ALARM / MANUAL */
    private String source;
    private Long assigneeId;
    private LocalDateTime dispatchAt;
    private LocalDateTime acceptAt;
    private LocalDateTime arriveAt;
    private LocalDateTime finishAt;
    private LocalDateTime deadline;
    private String status;
    private Integer isOverdue;
}
