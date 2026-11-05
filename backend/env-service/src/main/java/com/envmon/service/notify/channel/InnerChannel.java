package com.envmon.service.notify.channel;

import com.envmon.service.notify.NotifyChannel;
import com.envmon.service.notify.NotifyMessage;
import com.envmon.service.notify.NotifyResult;
import com.envmon.service.notify.Recipient;
import org.springframework.stereotype.Component;

/**
 * 站内消息：永远可用，也是外部通道失败后的降级落点。
 * 落库由 NotificationService 统一写 msg_send_log，这里不重复写。
 */
@Component
public class InnerChannel implements NotifyChannel {

    @Override
    public String code() {
        return "INNER";
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public NotifyResult send(NotifyMessage message, Recipient recipient) {
        return NotifyResult.ok(0);
    }
}
