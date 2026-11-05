package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分钟数据（时序域）。
 *
 * 不继承 TenantEntity：这张表没有 deleted 字段（时序数据不做软删除），
 * 且接入层没有租户上下文，tenant_id 由报文归属的站房显式带入。
 * 表已加入租户插件忽略名单，查询必须自己带 station_id。
 */
@Data
@TableName("ts_data_minute")
public class TsDataMinute implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long stationId;
    private String factorCode;

    /** 数据时间，已截断到分钟 */
    private LocalDateTime ts;

    /** 实测值 {因子}-Rtd */
    private BigDecimal value;

    /** 原始信号值，暂未采集 */
    private BigDecimal rawValue;

    /** 设备标记 N/D/C/M/T/Td */
    private String devFlag;

    /** 生产工况标记，暂未采集 */
    private String prodFlag;

    /** 1 有效 / 2 无效 / 3 未标记 */
    private Integer quality;

    /** 来源命令编码 2011 / 2051 / 2041 */
    private String sourceCn;

    /** hj212_frame.id，争议时回溯原文 */
    private Long frameId;
}
