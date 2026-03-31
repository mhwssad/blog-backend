package com.cybzacg.blogbackend.module.chat.service;

/**
 * 聊天指标埋点服务。
 */
public interface ChatMetricsService {
    void recordSend(String messageType, String result);

    void recordMediaProcess(String messageType, String result, long durationNanos);
}
