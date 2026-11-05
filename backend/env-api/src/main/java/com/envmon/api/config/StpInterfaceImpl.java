package com.envmon.api.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 角色来源：登录时写入 Sa-Token Session，避免每个请求查库 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId, false);
        if (session == null) {
            return Collections.emptyList();
        }
        Object roles = session.get("roles");
        return roles instanceof List ? (List<String>) roles : new ArrayList<String>();
    }
}
