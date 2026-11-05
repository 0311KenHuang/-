package com.envmon.service.notify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息通道配置。
 *
 * 生产密钥一律走环境变量，不写进配置文件；未配置时通道自动降级为 SKIPPED
 * 并在 msg_send_log 留痕，不会因为漏配把报警链路拖挂。
 */
@Data
@Component
@ConfigurationProperties(prefix = "notify")
public class NotifyProperties {

    private Feishu feishu = new Feishu();
    private Sms sms = new Sms();
    private Wxa wxa = new Wxa();

    /** 单通道最大尝试次数，失败后降级到站内消息 */
    private int maxRetry = 3;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 3000;

    /** 白天时段（含起点，不含终点）；夜间 P1 立即发短信 */
    private int dayStartHour = 8;
    private int dayEndHour = 20;

    /** 升级阈值：未认领多少分钟后触发 */
    private int p1FirstMinutes = 15;
    private int p1SecondMinutes = 30;
    private int p2FirstMinutes = 30;

    /** 卡片"查看详情"按钮地址，为空则不渲染按钮 */
    private String detailUrl = "";

    @Data
    public static class Feishu {
        /** 群机器人 Webhook，5 分钟即可接入；应用消息需另配 appId/appSecret */
        private String webhookUrl = "";
        private String appId = "";
        private String appSecret = "";
        private boolean enabled = true;
    }

    @Data
    public static class Sms {
        /** 短信网关 HTTP 地址，POST {phone, content}；为空表示未接入 */
        private String gatewayUrl = "";
        private boolean enabled = true;
    }

    @Data
    public static class Wxa {
        /** 小程序订阅消息网关；一次性订阅需用户授权，未绑定 openid 时跳过 */
        private String gatewayUrl = "";
        private boolean enabled = true;
    }
}
