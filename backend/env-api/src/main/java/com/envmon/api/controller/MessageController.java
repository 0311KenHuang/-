package com.envmon.api.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.envmon.common.api.PageResult;
import com.envmon.common.audit.Audit;
import com.envmon.dao.entity.MsgSendLog;
import com.envmon.service.notify.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final NotificationService notificationService;

    /** 发送日志：含 SENT / FAILED / SKIPPED，触达率从这里核对 */
    @GetMapping("/logs")
    public PageResult<MsgSendLog> logs(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return notificationService.logs(eventId, status, channel, page, pageSize);
    }

    /** 通道可用性：没收到消息时先看这里 */
    @GetMapping("/channels")
    public Map<String, Object> channels() {
        return notificationService.channelStatus();
    }

    /** 手工发一条测试消息，验证飞书/短信网关配置 */
    @PostMapping("/test")
    @SaCheckRole(value = {"SUP", "REG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "message", action = "TEST")
    public Map<String, Object> test(@RequestParam(defaultValue = "P1") String level) {
        return notificationService.sendTest(level);
    }

    /** 手工触发升级扫描 */
    @PostMapping("/escalate")
    @SaCheckRole(value = {"SUP", "REG", "MGR"}, mode = SaMode.OR)
    @Audit(module = "message", action = "ESCALATE")
    public Map<String, Object> escalate() {
        return notificationService.escalateAll();
    }
}
