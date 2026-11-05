package com.envmon.service.notify;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 一条待发送的消息：模板变量 + 渲染后的标题正文 */
@Data
public class NotifyMessage {

    private Long eventId;
    private String eventNo;
    /** ALARM_P1 / ALARM_P2 / ALARM_P3 / ALARM_RECOVER / ALARM_ESCALATE */
    private String eventType;
    private String level;
    private String ruleName;
    private Long stationId;
    private String stationName;
    private String factorCode;
    private BigDecimal value;
    private BigDecimal threshold;
    private LocalDateTime beginAt;
    /** 0 首报 / 1 T+15min / 2 T+30min */
    private Integer stage;

    private String title;
    private String body;
}
