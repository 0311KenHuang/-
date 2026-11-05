package com.envmon.service.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64)
    private String password;

    /** 多租户定位；单租户可留空 */
    private String tenantCode;
}
