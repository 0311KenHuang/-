package com.envmon.service.notify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 渠道决策与升级判定：纯函数，便于按 docs/04 §7.1 的矩阵做边界测试。
 *
 * P1 白天 08:00–20:00：飞书 + 小程序；15 分钟未认领加短信。
 * P1 夜间：飞书 + 短信立即发。
 * P2：飞书 + 小程序。P3：站内消息 + 日报。
 */
public final class NotifyPlanner {

    private NotifyPlanner() {
    }

    public static String eventType(String level) {
        return "ALARM_" + (level == null ? "P3" : level.trim().toUpperCase());
    }

    public static boolean isDay(int hour, NotifyProperties props) {
        return hour >= props.getDayStartHour() && hour < props.getDayEndHour();
    }

    public static List<String> channels(String eventType, String level, int hour, int stage,
                                        NotifyProperties props) {
        String lv = level == null ? "P3" : level.trim().toUpperCase();
        if ("ALARM_RECOVER".equals(eventType)) {
            return Arrays.asList("INNER", "FEISHU");
        }
        if ("ALARM_ESCALATE".equals(eventType)) {
            if ("P1".equals(lv)) {
                return Arrays.asList("FEISHU", "SMS");
            }
            return "P2".equals(lv) ? Collections.singletonList("FEISHU")
                    : Collections.singletonList("INNER");
        }
        if ("P1".equals(lv)) {
            if (!isDay(hour, props)) {
                return Arrays.asList("FEISHU", "SMS");
            }
            return stage >= 1 ? Arrays.asList("FEISHU", "SMS", "WXA")
                    : Arrays.asList("FEISHU", "WXA");
        }
        if ("P2".equals(lv)) {
            return Arrays.asList("FEISHU", "WXA");
        }
        return Collections.singletonList("INNER");
    }

    /** 未认领时长对应的目标升级阶段，0 表示还没到升级点 */
    public static int targetStage(String level, long openMinutes, NotifyProperties props) {
        String lv = level == null ? "P3" : level.trim().toUpperCase();
        if ("P1".equals(lv)) {
            if (openMinutes >= props.getP1SecondMinutes()) {
                return 2;
            }
            return openMinutes >= props.getP1FirstMinutes() ? 1 : 0;
        }
        if ("P2".equals(lv)) {
            return openMinutes >= props.getP2FirstMinutes() ? 1 : 0;
        }
        return 0;
    }
}
