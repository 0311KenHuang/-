package com.envmon.service.notify;

import lombok.Data;

/** 通道发送结果 */
@Data
public class NotifyResult {

    private boolean success;
    private boolean skipped;
    private String error;
    private int costMs;
    private int retryCnt;

    public static NotifyResult ok(int costMs) {
        NotifyResult r = new NotifyResult();
        r.setSuccess(true);
        r.setCostMs(costMs);
        return r;
    }

    public static NotifyResult skipped(String reason) {
        NotifyResult r = new NotifyResult();
        r.setSkipped(true);
        r.setError(reason);
        return r;
    }

    public static NotifyResult fail(String error, int costMs) {
        NotifyResult r = new NotifyResult();
        r.setError(error);
        r.setCostMs(costMs);
        return r;
    }
}
