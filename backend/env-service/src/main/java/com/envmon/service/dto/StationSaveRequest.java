package com.envmon.service.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class StationSaveRequest {

    @NotBlank(message = "站房编码不能为空")
    @Size(max = 32)
    private String stationCode;

    @NotBlank(message = "站房名称不能为空")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "站房类型不能为空")
    private String stationType;

    @NotNull(message = "所属企业不能为空")
    private Long enterpriseId;

    @NotNull(message = "所属区域不能为空")
    private Long regionId;

    private String lng;
    private String lat;
    private String address;
    private Long opsOrgId;
    private Long opsEngineerId;
}
