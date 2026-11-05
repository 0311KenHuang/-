package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/** 计划明细：一条明细对应一次现场作业，发布后生成任务 */
@Data
@TableName("ops_plan_item")
public class OpsPlanItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long planId;
    private String opsType;
    private LocalDate planDate;
    private Long assigneeId;
    /** 是否已实例化为任务 */
    private Integer done;
}
