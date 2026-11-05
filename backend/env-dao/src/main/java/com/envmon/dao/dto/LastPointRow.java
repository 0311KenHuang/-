package com.envmon.dao.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 某站房某因子最后一次上报时间，用于缺数判定 */
@Data
public class LastPointRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long stationId;
    private String factorCode;
    private LocalDateTime lastTs;
}
