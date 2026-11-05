package com.envmon.api.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.envmon.common.api.PageResult;
import com.envmon.dao.entity.SysRole;
import com.envmon.dao.entity.SysUser;
import com.envmon.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/user")
    @SaCheckRole(value = {"ORG", "REG"}, mode = SaMode.OR)
    public PageResult<SysUser> users(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "20") long pageSize) {
        return systemService.pageUsers(page, pageSize);
    }

    @GetMapping("/role")
    public List<SysRole> roles() {
        return systemService.listRoles();
    }
}
