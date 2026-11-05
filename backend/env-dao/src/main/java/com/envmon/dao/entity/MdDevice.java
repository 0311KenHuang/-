package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 设备：分析仪 / 数采仪 / 流量计，SN 是 HJ212-2025 的设备身份核心 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_device")
public class MdDevice extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private String deviceCode;
    private Long stationId;
    /** ANALYZER / DTU / FLOW / OTHER */
    private String category;
    private String sn;
    /** 数采仪站点编号（HJ212 报文 MN），直连场景下用于反查站房 */
    private String mn;
    private String vendor;
    private String model;
    private String firmware;
    private String rangeMin;
    private String rangeMax;
    /** REGISTERED / ACTIVATED / CONFIRMED */
    private String netStatus;
    /** IN_USE / STOPPED / REPLACED / SCRAPPED */
    private String status;
}
