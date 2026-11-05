package com.envmon.service.ops;

import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 任务状态机：DISPATCHED → ACCEPTED → ARRIVED → SUBMITTED → CLOSED。
 *
 * 每一步都写 ops_task_log，跨状态跳转直接拒绝；
 * RETURNED / CLOSED 是终态，不能再流转。
 */
public final class OpsStateMachine {

    public static final Set<String> FINAL = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("CLOSED", "RETURNED")));

    private OpsStateMachine() {
    }

    public static String next(String current, String action) {
        String from = current == null ? "DISPATCHED" : current;
        String act = action == null ? "" : action.trim().toUpperCase();
        if (FINAL.contains(from)) {
            throw BizException.of(ErrorCode.CONFLICT, "任务已结束，不能再流转");
        }
        if ("ACCEPT".equals(act)) {
            return require("DISPATCHED", from, act, "ACCEPTED");
        }
        if ("ARRIVE".equals(act)) {
            return require("ACCEPTED", from, act, "ARRIVED");
        }
        if ("SUBMIT".equals(act)) {
            return require("ARRIVED", from, act, "SUBMITTED");
        }
        if ("CLOSE".equals(act)) {
            return require("SUBMITTED", from, act, "CLOSED");
        }
        if ("TRANSFER".equals(act)) {
            if (!Arrays.asList("DISPATCHED", "ACCEPTED", "ARRIVED").contains(from)) {
                throw BizException.of(ErrorCode.CONFLICT, "当前状态不允许改派");
            }
            return "DISPATCHED";
        }
        if ("RETURN".equals(act)) {
            if (!Arrays.asList("DISPATCHED", "ACCEPTED").contains(from)) {
                throw BizException.of(ErrorCode.CONFLICT, "当前状态不允许退回");
            }
            return "RETURNED";
        }
        throw BizException.of(ErrorCode.BAD_REQUEST, "未知的流转动作: " + act);
    }

    /** BR-04：审核人不得为执行人（系统自动审核的 0 号用户除外） */
    public static void assertAuditorAllowed(Long engineerId, Long auditorId) {
        if (auditorId != null && auditorId != 0L && auditorId.equals(engineerId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "BR-04：审核人不得为执行人");
        }
    }

    /** 超期判定只针对未终结、未提交的任务 */
    public static boolean countable(String status) {
        return status != null && !FINAL.contains(status) && !"SUBMITTED".equals(status);
    }

    private static String require(String expected, String from, String action, String target) {
        if (!expected.equals(from)) {
            throw BizException.of(ErrorCode.CONFLICT,
                    "状态不符：" + action + " 要求 " + expected + "，当前 " + from);
        }
        return target;
    }
}
