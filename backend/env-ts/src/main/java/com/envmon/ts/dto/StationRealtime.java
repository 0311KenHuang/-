package com.envmon.ts.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 工作台"站房实时值"一行：站房 + 各因子最新读数 */
@Data
public class StationRealtime implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long stationId;
    private String stationCode;
    private String stationName;
    private String bizStatus;
    /** 该站房所有因子中最新的数据时间 */
    private LocalDateTime updatedAt;
    private List<RealTimeValue> factors = new ArrayList<RealTimeValue>();
}
