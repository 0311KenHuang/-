package com.envmon.common.audit;

import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** @Audit 注解的切面：业务成功后异步写审计，失败不影响主流程 */
@Slf4j
@Aspect
@Component
public class AuditAspect {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired(required = false)
    private AuditWriter auditWriter;

    /** 变更前快照提供者：业务模块实现后才会有，没有就只记 after */
    @Autowired(required = false)
    private AuditBeforeProvider beforeProvider;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        // 必须在业务方法执行前取旧值：执行后库里已经是新值，before 就永远补不回来了
        String beforeJson = snapshotBefore(audit, pjp.getArgs());
        Object result = pjp.proceed();
        if (auditWriter == null) {
            return result;
        }
        try {
            AuditLogEntry entry = new AuditLogEntry();
            TenantContext ctx = TenantContextHolder.get();
            if (ctx != null) {
                entry.setTenantId(ctx.getTenantId());
                entry.setUserId(ctx.getUserId());
                entry.setUserName(ctx.getUsername());
            }
            entry.setModule(audit.module());
            entry.setAction(audit.action());
            entry.setBizId(extractBizId(result, pjp.getArgs()));
            entry.setBeforeJson(beforeJson);
            entry.setAfterJson(toJson(pjp.getArgs()));
            fillRequestInfo(entry);
            auditWriter.write(entry);
        } catch (Exception e) {
            // 审计失败绝不能影响业务
            log.warn("审计写入失败: {}", e.getMessage());
        }
        return result;
    }

    /** 前置快照失败只降级为"没有 before"，不能打断业务 */
    private String snapshotBefore(Audit audit, Object[] args) {
        if (beforeProvider == null) {
            return null;
        }
        try {
            Object snapshot = beforeProvider.snapshot(audit.module(), audit.action(), args);
            return toJson(snapshot);
        } catch (Exception e) {
            log.warn("审计前置快照失败 module={} action={}: {}", audit.module(), audit.action(), e.getMessage());
            return null;
        }
    }

    private String extractBizId(Object result, Object[] args) {
        Object id = null;
        if (result != null) {
            try {
                id = result.getClass().getMethod("getId").invoke(result);
            } catch (Exception ignored) {
                // 返回值没有 getId，退回入参
            }
        }
        if (id == null && args != null && args.length > 0
                && (args[0] instanceof Number || args[0] instanceof CharSequence)) {
            id = args[0];
        }
        if (id == null) {
            return null;
        }
        String s = String.valueOf(id);
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    /** 只保留可读的关键字段；密码类字段必须脱敏后才能入审计 */
    private String toJson(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            Map<String, Object> safe = new LinkedHashMap<String, Object>();
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if (a == null || a instanceof HttpServletRequest) {
                    continue;
                }
                safe.put("arg" + i, a);
            }
            return clip(maskSensitive(MAPPER.writeValueAsString(safe)));
        } catch (Exception e) {
            return null;
        }
    }

    /** 单个对象序列化（变更前快照）：与入参共用脱敏与长度上限 */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return clip(maskSensitive(MAPPER.writeValueAsString(value)));
        } catch (Exception e) {
            return null;
        }
    }

    /** 审计列是 TEXT，但单条留痕不该无限膨胀：超长直接截断并留标记 */
    private String clip(String json) {
        if (json == null || json.length() <= 4000) {
            return json;
        }
        return json.substring(0, 4000) + "...(truncated)";
    }

    /** 审计里绝不能出现明文口令/令牌 */
    private String maskSensitive(String json) {
        if (json == null) {
            return null;
        }
        return json.replaceAll(
                "(?i)(\"(?:password|passwordHash|token|secret|credential)[A-Za-z]*\"\\s*:\\s*\")[^\"]*(\")",
                "$1***$2");
    }

    private void fillRequestInfo(AuditLogEntry entry) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest req = attrs.getRequest();
        entry.setIp(clientIp(req));
        entry.setUa(req.getHeader("User-Agent"));
    }

    private String clientIp(HttpServletRequest req) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String h : headers) {
            String v = req.getHeader(h);
            if (v != null && !v.trim().isEmpty() && !"unknown".equalsIgnoreCase(v)) {
                int idx = v.indexOf(',');
                return idx > 0 ? v.substring(0, idx).trim() : v.trim();
            }
        }
        return req.getRemoteAddr();
    }
}
