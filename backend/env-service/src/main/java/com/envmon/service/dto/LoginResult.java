package com.envmon.service.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LoginResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String username;
        private String realName;
        private Long orgId;
        private List<String> roles;
        private String dataScope;
    }
}
