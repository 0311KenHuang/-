package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 运维记录：一个任务一份，审核通过后不可再改 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ops_record")
public class OpsRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private String recordNo;
    private Long taskId;
    private Long stationId;
    private String opsType;
    private Long engineerId;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    /** 应产生的数据标记区间，供标记核验使用 */
    private LocalDateTime markBegin;
    private LocalDateTime markEnd;
    private String conclusion;
    private LocalDate nextPlanHint;
    /** DRAFT / SUBMITTED / APPROVED / REJECTED */
    private String status;
    private String signEngineer;
    private String signAuditor;
}
