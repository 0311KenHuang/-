package com.envmon.service.notify;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyPlannerTest {

    private final NotifyProperties props = new NotifyProperties();

    @Test
    void P1白天首报走飞书加小程序() {
        assertEquals(Arrays.asList("FEISHU", "WXA"),
                NotifyPlanner.channels("ALARM_P1", "P1", 14, 0, props));
    }

    @Test
    void P1白天十五分钟未认领加短信() {
        assertEquals(Arrays.asList("FEISHU", "SMS", "WXA"),
                NotifyPlanner.channels("ALARM_P1", "P1", 14, 1, props));
    }

    @Test
    void P1夜间立即发短信() {
        assertEquals(Arrays.asList("FEISHU", "SMS"),
                NotifyPlanner.channels("ALARM_P1", "P1", 23, 0, props));
        assertEquals(Arrays.asList("FEISHU", "SMS"),
                NotifyPlanner.channels("ALARM_P1", "P1", 3, 0, props));
    }

    @Test
    void P2与P3的渠道组合() {
        assertEquals(Arrays.asList("FEISHU", "WXA"),
                NotifyPlanner.channels("ALARM_P2", "P2", 10, 0, props));
        assertEquals(Arrays.asList("INNER"),
                NotifyPlanner.channels("ALARM_P3", "P3", 10, 0, props));
    }

    @Test
    void 恢复与升级的渠道组合() {
        assertEquals(Arrays.asList("INNER", "FEISHU"),
                NotifyPlanner.channels("ALARM_RECOVER", "P1", 10, 0, props));
        assertEquals(Arrays.asList("FEISHU", "SMS"),
                NotifyPlanner.channels("ALARM_ESCALATE", "P1", 10, 1, props));
    }

    @Test
    void P1升级阈值十五与三十分钟() {
        assertEquals(0, NotifyPlanner.targetStage("P1", 14, props));
        assertEquals(1, NotifyPlanner.targetStage("P1", 15, props));
        assertEquals(2, NotifyPlanner.targetStage("P1", 30, props));
    }

    @Test
    void P2升级阈值三十分钟_P3不升级() {
        assertEquals(0, NotifyPlanner.targetStage("P2", 29, props));
        assertEquals(1, NotifyPlanner.targetStage("P2", 30, props));
        assertEquals(0, NotifyPlanner.targetStage("P3", 300, props));
    }

    @Test
    void 白天时段边界() {
        assertTrue(NotifyPlanner.isDay(8, props));
        assertTrue(NotifyPlanner.isDay(19, props));
        assertFalse(NotifyPlanner.isDay(20, props));
        assertFalse(NotifyPlanner.isDay(7, props));
    }

    @Test
    void 事件类型由级别推导() {
        assertEquals("ALARM_P1", NotifyPlanner.eventType("P1"));
        assertEquals("ALARM_P3", NotifyPlanner.eventType(null));
    }
}
