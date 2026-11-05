package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 现场打卡：与站房坐标距离超过 300m 记异地打卡 */
@Data
@TableName("ops_checkin")
public class OpsCheckin implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long taskId;
    private Long userId;
    private Long stationId;
    private BigDecimal lng;
    private BigDecimal lat;
    private Integer distanceM;
    private String photoKey;
    private LocalDateTime inAt;
    private LocalDateTime outAt;
    private Integer isValid;
}
