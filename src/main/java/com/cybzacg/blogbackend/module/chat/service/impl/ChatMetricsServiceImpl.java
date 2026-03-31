package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.module.chat.service.ChatMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * 聊天指标埋点实现。
 */
@Service
public class ChatMetricsServiceImpl implements ChatMetricsService {
    private final MeterRegistry meterRegistry;

    public ChatMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordSend(String messageType, String result) {
        meterRegistry.counter("chat.message.send.total", "messageType", normalize(messageType), "result", normalize(result))
                .increment();
    }

    @Override
    public void recordMediaProcess(String messageType, String result, long durationNanos) {
        Timer.builder("chat.media.process.duration")
                .tags("messageType", normalize(messageType), "result", normalize(result))
                .register(meterRegistry)
                .record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
