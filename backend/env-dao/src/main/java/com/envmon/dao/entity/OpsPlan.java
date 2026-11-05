package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 运维计划：一站房一月一份，明细实例化成任务 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ops_plan")
public class OpsPlan extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private String planNo;
    private Long stationId;
    /** 月份 YYYY-MM */
    private String period;
    private String planType;
    private LocalDate plannedDate;
    /** AUTO / MANUAL */
    private String source;
    /** DRAFT / SUBMITTED / APPROVED / DONE / MISSED */
    private String status;
    private Long approveBy;
    private LocalDateTime approveAt;
    private Long createdBy;
}
