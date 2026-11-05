package com.envmon.common.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审计切面的两个关键约束：
 *   1. 前置快照必须发生在业务方法执行之前（否则库裡已是新值）；
 *   2. 前置快照或审计落库失败，不能影响业务返回值。
 */
class AuditAspectTest {

    private final AuditAspect aspect = new AuditAspect();
    private final AuditWriter writer = mock(AuditWriter.class);
    private final AuditBeforeProvider beforeProvider = mock(AuditBeforeProvider.class);
    private final ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

    private final Audit audit = auditOf("station", "UPDATE");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aspect, "auditWriter", writer);
        ReflectionTestUtils.setField(aspect, "beforeProvider", beforeProvider);
        when(pjp.getArgs()).thenReturn(new Object[]{100L});
    }

    private Audit auditOf(String module, String action) {
        Audit annotation = mock(Audit.class);
        when(annotation.module()).thenReturn(module);
        when(annotation.action()).thenReturn(action);
        return annotation;
    }

    @Test
    void shouldSnapshotBeforeBusinessRuns() throws Throwable {
        Map<String, Object> old = new HashMap<String, Object>();
        old.put("id", 100);
        old.put("name", "旧站房名");
        when(beforeProvider.snapshot("station", "UPDATE", new Object[]{100L})).thenReturn(old);
        when(pjp.proceed()).thenReturn("ok");

        assertSame("ok", aspect.around(pjp, audit));

        InOrder order = inOrder(beforeProvider, pjp);
        order.verify(beforeProvider).snapshot("station", "UPDATE", new Object[]{100L});
        order.verify(pjp).proceed();

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(writer).write(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertTrue(entry.getBeforeJson().contains("旧站房名"), "before 应记录变更前内容");
        assertEquals("station", entry.getModule());
        assertEquals("UPDATE", entry.getAction());
        assertEquals("100", entry.getBizId());
    }

    @Test
    void shouldStillRunBusinessWhenSnapshotFails() throws Throwable {
        when(beforeProvider.snapshot(any(), any(), any())).thenThrow(new RuntimeException("db down"));
        when(pjp.proceed()).thenReturn("ok");

        assertSame("ok", aspect.around(pjp, audit));

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(writer).write(captor.capture());
        assertNull(captor.getValue().getBeforeJson(), "快照失败时只影响 before 字段");
        assertTrue(captor.getValue().getAfterJson() != null, "after 仍要记录");
    }

    @Test
    void shouldMaskSensitiveFieldsInSnapshot() throws Throwable {
        Map<String, Object> old = new HashMap<String, Object>();
        old.put("password", "plain-text");
        when(beforeProvider.snapshot(any(), any(), any())).thenReturn(old);
        when(pjp.proceed()).thenReturn("ok");

        aspect.around(pjp, audit);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(writer).write(captor.capture());
        assertTrue(captor.getValue().getBeforeJson().contains("***"), "快照里的口令必须脱敏");
    }

    @Test
    void shouldNotTouchProviderWhenThereIsNone() throws Throwable {
        ReflectionTestUtils.setField(aspect, "beforeProvider", null);
        when(pjp.proceed()).thenReturn("ok");

        aspect.around(pjp, audit);

        verify(writer).write(any(AuditLogEntry.class));
        verify(beforeProvider, never()).snapshot(any(), any(), any());
    }

    @Test
    void shouldNotBreakBusinessWhenWriterFails() throws Throwable {
        when(beforeProvider.snapshot(any(), any(), any())).thenReturn(Collections.emptyMap());
        when(pjp.proceed()).thenReturn("ok");
        org.mockito.Mockito.doThrow(new RuntimeException("queue full")).when(writer).write(any());

        assertSame("ok", aspect.around(pjp, audit));
    }
}
