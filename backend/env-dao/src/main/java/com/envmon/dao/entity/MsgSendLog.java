package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 消息发送日志：只追加，含跳过与降级，触达率从这里统计 */
@Data
@TableName("msg_send_log")
public class MsgSendLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long eventId;
    private String channel;
    private String receiver;
    private String templateCode;
    /** SENT / FAILED / SKIPPED / RETRY */
    private String status;
    /** 0 首报 / 1 T+15min / 2 T+30min */
    private Integer stage;
    private Integer retryCnt;
    private String errorMsg;
    private Integer costMs;
    private String content;
    private LocalDateTime sentAt;
}
