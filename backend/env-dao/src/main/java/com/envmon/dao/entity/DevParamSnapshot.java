package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 工作参数快照：只追加，参数历史可回溯 */
@Data
@TableName("dev_param_snapshot")
public class DevParamSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long stationId;
    private Long deviceId;
    private String mn;
    private String paramCode;
    private String paramValue;
    private LocalDateTime collectedAt;
    private String source;
    private Long frameId;
}
