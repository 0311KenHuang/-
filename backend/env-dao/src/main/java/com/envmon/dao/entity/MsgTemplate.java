package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 消息模板：按 event_type + channel 匹配，{{占位符}} 由模板服务渲染 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("msg_template")
public class MsgTemplate extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private String eventType;
    /** FEISHU / WXA / SMS / INNER */
    private String channel;
    private String titleTpl;
    private String bodyTpl;
    /** 飞书交互卡片模板，预留 */
    private String cardJson;
    private Integer enabled;
}
