package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 站房：最小运维管理单元 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_station")
public class MdStation extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 可读业务编码，如 FHZY-S-01 */
    private String stationCode;
    private String name;
    /** WASTE_WATER / WASTE_GAS / VOC / INCINERATION / AIR / GROUNDWATER */
    private String stationType;
    private Long enterpriseId;
    private Long regionId;
    private String lng;
    private String lat;
    private String address;
    /** NORMAL / ABNORMAL / STOPPED / MAINTAINING */
    private String bizStatus;
    private String stopReason;
    /** 责任运维单位 */
    private Long opsOrgId;
    private Long opsEngineerId;
    private Integer isFocus;
    private String qrToken;
}
