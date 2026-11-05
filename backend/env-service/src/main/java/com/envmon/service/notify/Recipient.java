package com.envmon.service.notify;

import lombok.Data;

/** 接收人解析结果：责任工程师 → 值班人 → 角色兜底 */
@Data
public class Recipient {

    private Long userId;
    private String name;
    private String phone;
    /** 小程序订阅需要，未绑定为空 */
    private String openid;
    /** ENGINEER / DUTY / ROLE / UNKNOWN */
    private String source;

    public String display() {
        if (name == null || name.trim().isEmpty()) {
            return phone == null || phone.trim().isEmpty() ? "未解析到责任人" : phone;
        }
        return phone == null || phone.trim().isEmpty() ? name : name + "(" + phone + ")";
    }
}
