package com.envmon.service.ops;

import com.envmon.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpsFlowTest {

    @Test
    void 任务按接单到场提交关闭顺序流转() {
        String s = "DISPATCHED";
        s = OpsStateMachine.next(s, "ACCEPT");
        assertEquals("ACCEPTED", s);
        s = OpsStateMachine.next(s, "ARRIVE");
        assertEquals("ARRIVED", s);
        s = OpsStateMachine.next(s, "SUBMIT");
        assertEquals("SUBMITTED", s);
        s = OpsStateMachine.next(s, "CLOSE");
        assertEquals("CLOSED", s);
    }

    @Test
    void 跨状态跳转被拒绝() {
        assertThrows(BizException.class, () -> OpsStateMachine.next("DISPATCHED", "ARRIVE"));
        assertThrows(BizException.class, () -> OpsStateMachine.next("ACCEPTED", "SUBMIT"));
        assertThrows(BizException.class, () -> OpsStateMachine.next("CLOSED", "ACCEPT"));
        assertThrows(BizException.class, () -> OpsStateMachine.next("DISPATCHED", "UNKNOWN"));
    }

    @Test
    void 改派回到待接单_退回进终态() {
        assertEquals("DISPATCHED", OpsStateMachine.next("ARRIVED", "TRANSFER"));
        assertEquals("RETURNED", OpsStateMachine.next("ACCEPTED", "RETURN"));
        assertFalse(OpsStateMachine.countable("CLOSED"));
        assertFalse(OpsStateMachine.countable("SUBMITTED"));
        assertTrue(OpsStateMachine.countable("ACCEPTED"));
    }

    @Test
    void 电子围栏三百米判定() {
        assertEquals(0, GeoUtil.distanceMeters(103.9301, 30.5712, 103.9301, 30.5712));
        assertTrue(GeoUtil.inFence(103.9301, 30.5712, 103.9311, 30.5712, 300));
        // 1 个纬度约 111 公里，必然超出围栏
        assertFalse(GeoUtil.inFence(103.9301, 30.5712, 103.9301, 31.5712, 300));
    }

    @Test
    void 审核人不得为执行人() {
        assertThrows(BizException.class, () -> OpsStateMachine.assertAuditorAllowed(7L, 7L));
        OpsStateMachine.assertAuditorAllowed(7L, 8L);
        OpsStateMachine.assertAuditorAllowed(7L, 0L);
    }
}
