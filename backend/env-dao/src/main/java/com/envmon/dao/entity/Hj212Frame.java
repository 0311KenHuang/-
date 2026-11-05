package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 原始报文：争议时唯一证据，不对租户插件开放（接入层无租户上下文） */
@Data
@TableName("hj212_frame")
public class Hj212Frame implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String mn;
    private String cn;
    private String qn;
    /** 归属站房，按 MN 解析；未注册设备为空 */
    private Long stationId;
    private Integer crcOk;
    /** TCP / HTTP */
    private String source;
    private String raw;
    /** 解析后的 CP 键值对，JSON 字符串（多 DataTime 组会折叠，仅作查询便利） */
    private String params;
    /** CP 原文（不含 &&），重算分钟数据时按原样解析，避免多组报文丢数 */
    private String cp;
    private LocalDateTime receivedAt;
}
