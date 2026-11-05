package com.envmon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envmon.common.api.PageResult;
import com.envmon.dao.entity.SysRole;
import com.envmon.dao.entity.SysUser;
import com.envmon.dao.mapper.SysRoleMapper;
import com.envmon.dao.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public PageResult<SysUser> pageUsers(long page, long pageSize) {
        Page<SysUser> p = userMapper.selectPage(new Page<SysUser>(page, pageSize),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId));
        // 永远不要把密码哈希返回给前端
        for (SysUser u : p.getRecords()) {
            u.setPasswordHash(null);
        }
        return PageResult.of(p.getRecords(), p.getTotal(), page, pageSize);
    }

    public List<SysRole> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }
}
