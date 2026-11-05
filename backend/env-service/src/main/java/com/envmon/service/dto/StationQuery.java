package com.envmon.service.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/** 站房分页查询条件 */
@Data
public class StationQuery {

    @Min(1)
    private Long page = 1L;

    @Min(1)
    @Max(200)
    private Long pageSize = 20L;

    private Long enterpriseId;
    private Long regionId;
    private String stationType;
    private String bizStatus;

    /** 站房名 / 编码 模糊搜索 */
    private String keyword;
}
