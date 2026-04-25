package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.module.chat.service.ChatMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 聊天指标埋点实现。
 */
@Service
public class ChatMetricsServiceImpl implements ChatMetricsService {
    private final MeterRegistry meterRegistry;

    public ChatMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录消息发送指标。
     *
     * @param messageType 消息类型（text/file/image/voice）
     * @param result      发送结果（success/business_error/system_error）
     */
    @Override
    public void recordSend(String messageType, String result) {
        meterRegistry.counter("chat.message.send.total", "messageType", normalize(messageType), "result", normalize(result))
                .increment();
    }

    /**
     * 记录媒体文件异步处理耗时指标。
     *
     * @param messageType   消息类型（image/voice）
     * @param result        处理结果（success/failed/skipped）
     * @param durationNanos 处理耗时（纳秒）
     */
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
