package com.envmon.service.alarm.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 报警事件列表项：把站房名、规则名拼好，前端不用再查一次 */
@Data
public class AlarmEventView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String eventNo;
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private Long stationId;
    private String stationCode;
    private String stationName;
    private String factorCode;
    private String level;
    private BigDecimal value;
    private BigDecimal threshold;
    private LocalDateTime beginAt;
    private LocalDateTime endAt;
    private String status;
    private Long ownerId;
}
