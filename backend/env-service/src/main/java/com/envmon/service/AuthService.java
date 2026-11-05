package com.envmon.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.SysTenant;
import com.envmon.dao.entity.SysUser;
import com.envmon.dao.mapper.SysTenantMapper;
import com.envmon.dao.mapper.SysUserMapper;
import com.envmon.service.dto.LoginRequest;
import com.envmon.service.dto.LoginResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** 防爆破：同一用户名连续失败 N 次后锁定一段时间 */
    private static final int FAIL_LIMIT = 5;
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(15);

    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;
    /** Redis 不可用时跳过限流（fail-open），不阻塞登录主流程 */
    private final ObjectProvider<StringRedisTemplate> redisProvider;

    public LoginResult login(LoginRequest req) {
        assertNotLocked(req.getUsername());
        // 登录时还没有租户上下文，只能走忽略租户的查询
        SysUser user;
        if (StringUtils.isNotBlank(req.getTenantCode())) {
            // 传了租户编码：先定位租户，再按 username+tenantId 精确取人，
            // 避免同名用户命中别的租户被误判"密码错误"
            SysTenant tenant = tenantMapper.selectOne(Wrappers.<SysTenant>lambdaQuery()
                    .eq(SysTenant::getTenantCode, req.getTenantCode()));
            if (tenant == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "租户不存在");
            }
            user = userMapper.selectByUsernameAndTenant(req.getUsername(), tenant.getId());
        } else {
            // 未传租户编码：只有全局唯一用户名才允许放行，
            // 否则可能登录到错误租户的同名账号（P0：多租户登录歧义）
            int sameName = userMapper.countByUsernameIgnoreTenant(req.getUsername());
            if (sameName > 1) {
                throw new BizException(ErrorCode.UNAUTHORIZED.getCode(),
                        "该用户名存在于多个租户，请填写租户编码后登录");
            }
            user = userMapper.selectByUsernameIgnoreTenant(req.getUsername());
        }
        // 统一提示，不暴露"用户是否存在"
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "账号已停用或锁定");
        }
        if (!ENCODER.matches(req.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(req.getUsername());
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        clearLoginFailure(req.getUsername());

        // 认证通过后再建立租户上下文；注意要复用已有实例，否则会丢掉 traceId
        TenantContext ctx = TenantContextHolder.get();
        if (ctx == null) {
            ctx = new TenantContext();
            TenantContextHolder.set(ctx);
        }
        ctx.setTenantId(user.getTenantId());
        ctx.setUserId(user.getId());
        ctx.setUsername(user.getUsername());
        ctx.setOrgId(user.getOrgId());
        ctx.setRegionId(user.getRegionId());
        TenantContextHolder.set(ctx);

        List<String> roles = userMapper.selectRoleCodes(user.getId());
        String scope = userMapper.selectTopDataScope(user.getId());
        ctx.setRoles(roles);
        ctx.setDataScope(scope == null ? "SELF" : scope);

        StpUtil.login(user.getId());
        StpUtil.getSession().set("tenantId", user.getTenantId());
        StpUtil.getSession().set("roles", roles);
        StpUtil.getSession().set("dataScope", ctx.getDataScope());
        StpUtil.getSession().set("username", user.getUsername());
        // Sa-Token 会话不能存 null（SaSession.set 内部 ConcurrentHashMap.putVal 会 NPE），
        // 归属字段为空时跳过写入，读取侧按缺省处理
        if (user.getOrgId() != null) {
            StpUtil.getSession().set("orgId", user.getOrgId());
        }
        if (user.getRegionId() != null) {
            StpUtil.getSession().set("regionId", user.getRegionId());
        }

        userMapper.updateById(buildLoginTimePatch(user.getId()));

        LoginResult result = new LoginResult();
        result.setToken(StpUtil.getTokenValue());
        LoginResult.UserInfo info = new LoginResult.UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setOrgId(user.getOrgId());
        info.setRoles(roles);
        info.setDataScope(ctx.getDataScope());
        result.setUser(info);
        return result;
    }

    private SysUser buildLoginTimePatch(Long userId) {
        SysUser patch = new SysUser();
        patch.setId(userId);
        patch.setLastLoginAt(LocalDateTime.now());
        return patch;
    }

    // ---------- 登录防爆破（Redis 计数，fail-open） ----------

    private void assertNotLocked(String username) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            String cnt = redis.opsForValue().get(failKey(username));
            if (cnt != null && Integer.parseInt(cnt) >= FAIL_LIMIT) {
                throw new BizException(ErrorCode.UNAUTHORIZED.getCode(),
                        "登录失败次数过多，请 " + FAIL_WINDOW.toMinutes() + " 分钟后再试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("登录限流检查失败（跳过）: {}", e.getMessage());
        }
    }

    private void recordLoginFailure(String username) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            String key = failKey(username);
            Long n = redis.opsForValue().increment(key);
            if (n != null && n == 1L) {
                redis.expire(key, FAIL_WINDOW);
            }
        } catch (Exception e) {
            log.warn("登录失败计数写入失败（跳过）: {}", e.getMessage());
        }
    }

    private void clearLoginFailure(String username) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.delete(failKey(username));
        } catch (Exception e) {
            log.warn("登录失败计数清理失败（跳过）: {}", e.getMessage());
        }
    }

    private String failKey(String username) {
        return "login:fail:" + username.trim().toLowerCase();
    }

    /** 供 profile 接口使用：返回当前会话信息 */
    public LoginResult.UserInfo currentUser() {
        TenantContext ctx = TenantContextHolder.get();
        LoginResult.UserInfo info = new LoginResult.UserInfo();
        if (ctx == null) {
            return info;
        }
        info.setId(ctx.getUserId());
        info.setUsername(ctx.getUsername());
        info.setOrgId(ctx.getOrgId());
        info.setRoles(ctx.getRoles() == null ? new ArrayList<String>() : ctx.getRoles());
        info.setDataScope(ctx.getDataScope());
        return info;
    }
}
