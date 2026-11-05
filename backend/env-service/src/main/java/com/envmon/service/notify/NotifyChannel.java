package com.envmon.service.notify;

/** 消息通道适配器：加渠道只加实现，不动发送主流程 */
public interface NotifyChannel {

    /** FEISHU / WXA / SMS / INNER */
    String code();

    /** 是否具备发送条件：未配置时主流程记 SKIPPED，不抛异常 */
    boolean configured();

    NotifyResult send(NotifyMessage message, Recipient recipient);
}
