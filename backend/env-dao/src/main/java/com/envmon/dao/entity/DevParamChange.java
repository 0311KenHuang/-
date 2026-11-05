package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 参数变更留痕：改了什么、从什么改到什么、哪一帧报文带来的 */
@Data
@TableName("dev_param_change")
public class DevParamChange implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long stationId;
    private Long deviceId;
    private String paramCode;
    private String oldValue;
    private String newValue;
    private LocalDateTime changeTime;
    private Long operator;
    private String evidenceKey;
    private Long frameId;
    private Long eventId;
}
