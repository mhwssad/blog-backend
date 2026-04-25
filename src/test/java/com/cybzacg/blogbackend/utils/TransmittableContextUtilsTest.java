package com.cybzacg.blogbackend.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class TransmittableContextUtilsTest {

    @AfterEach
    void tearDown() {
        TransmittableContextUtils.clear();
    }

    @Test
    void shouldPutGetRemoveAndClearContextValue() {
        TransmittableContextUtils.put("traceId", "trace-1");
        TransmittableContextUtils.put("userId", 99L);

        assertEquals("trace-1", TransmittableContextUtils.getString("traceId"));
        assertEquals(99L, TransmittableContextUtils.getLong("userId"));

        TransmittableContextUtils.remove("traceId");

        assertTrue(TransmittableContextUtils.get("traceId").isEmpty());

        TransmittableContextUtils.clear();

        assertTrue(TransmittableContextUtils.snapshot().isEmpty());
    }

    @Test
    void shouldReplaceAndSnapshotContextSafely() {
        TransmittableContextUtils.replace(Map.of("traceId", "trace-2", "operatorId", 1001L));

        Map<String, Object> snapshot = TransmittableContextUtils.snapshot();

        assertEquals("trace-2", snapshot.get("traceId"));
        assertEquals(1001L, snapshot.get("operatorId"));
        assertThrowsUnsupported(snapshot);
    }

    @Test
    void runWithContextShouldRestorePreviousContextAfterExecution() {
        TransmittableContextUtils.put("traceId", "parent-trace");

        TransmittableContextUtils.runWithContext(Map.of("traceId", "child-trace", "tenantId", 7L), () -> {
            assertEquals("child-trace", TransmittableContextUtils.getString("traceId"));
            assertEquals(7L, TransmittableContextUtils.getLong("tenantId"));
        });

        assertEquals("parent-trace", TransmittableContextUtils.getString("traceId"));
        assertNull(TransmittableContextUtils.getLong("tenantId"));
    }

    @Test
    void callWithContextShouldReturnValueAndRestorePreviousContext() throws Exception {
        TransmittableContextUtils.put("traceId", "origin-trace");

        String traceId = TransmittableContextUtils.callWithContext(Map.of("traceId", "call-trace"), () ->
                TransmittableContextUtils.getString("traceId"));

        assertEquals("call-trace", traceId);
        assertEquals("origin-trace", TransmittableContextUtils.getString("traceId"));
    }

    @Test
    void wrappedRunnableAndCallableShouldTransmitContextToThreadPool() throws ExecutionException, InterruptedException {
        TransmittableContextUtils.replace(Map.of("traceId", "async-trace", "userId", 88L));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Future<?> runnableFuture = executorService.submit(TransmittableContextUtils.wrap(() -> {
                assertEquals("async-trace", TransmittableContextUtils.getString("traceId"));
                assertEquals(88L, TransmittableContextUtils.getLong("userId"));
            }));

            Future<String> callableFuture = executorService.submit(TransmittableContextUtils.wrap(
                    (Callable<String>) () -> TransmittableContextUtils.getString("traceId")));

            runnableFuture.get();
            assertEquals("async-trace", callableFuture.get());
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void getLongShouldReturnNullForInvalidValue() {
        TransmittableContextUtils.put("userId", "invalid");

        assertNull(TransmittableContextUtils.getLong("userId"));
        assertFalse(TransmittableContextUtils.get("userId").isEmpty());
    }

    private void assertThrowsUnsupported(Map<String, Object> snapshot) {
        boolean unsupportedThrown = false;
        try {
            snapshot.put("extra", "value");
        } catch (UnsupportedOperationException ex) {
            unsupportedThrown = true;
        }
        assertTrue(unsupportedThrown);
    }
}
