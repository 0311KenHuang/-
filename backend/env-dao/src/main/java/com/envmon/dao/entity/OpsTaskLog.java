package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/** 任务流转日志：谁在什么时间把任务推进到哪一步 */
@Data
@TableName("ops_task_log")
public class OpsTaskLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long taskId;
    private String action;
    private Long operator;
    private String remark;
}
