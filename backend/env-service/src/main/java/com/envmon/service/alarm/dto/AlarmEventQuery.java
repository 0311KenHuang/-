package com.envmon.service.alarm.dto;

import lombok.Data;

import java.io.Serializable;

/** 报警事件查询条件 */
@Data
public class AlarmEventQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private int page = 1;
    private int pageSize = 20;
    /** OPEN / ACK / PROCESSING / CLOSED / FALSE_ALARM，空表示全部 */
    private String status;
    /** P1 / P2 / P3 */
    private String level;
    private Long stationId;
}
