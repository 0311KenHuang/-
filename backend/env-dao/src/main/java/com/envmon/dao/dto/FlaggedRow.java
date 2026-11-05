package com.envmon.dao.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 某站房某类异常标记的起止时间，用于标记 ⇄ 工单核验 */
@Data
public class FlaggedRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long stationId;
    private String devFlag;
    private LocalDateTime minTs;
    private LocalDateTime maxTs;
}
