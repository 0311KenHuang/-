package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 站房因子配置：决定上报哪些因子、量程与限值 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_station_factor")
public class MdStationFactor extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private Long stationId;
    /** HJ212 参数编码，如 a21026 */
    private String factorCode;
    private Long deviceId;
    private String unit;
    private String rangeMin;
    private String rangeMax;
    /** 执行限值 */
    private String stdLimit;
    private Integer isMain;
    private Integer sort;
}
