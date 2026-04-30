package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.module.chat.message.service.impl.ChatMessageGovernanceServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageGovernanceServiceImplTest {
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private SysConfigService sysConfigService;

    private ChatMessageGovernanceServiceImpl governanceService;

    @BeforeEach
    void setUp() {
        governanceService = new ChatMessageGovernanceServiceImpl(redisOperator, sysConfigService, new SimpleMeterRegistry());
    }

    @Test
    void validateTextMessageShouldRejectSensitiveWord() {
        when(sysConfigService.getValueOrDefault("chat.send.rate-limit.per-minute", "30")).thenReturn("30");
        when(sysConfigService.getValueOrDefault("chat.sensitive-words", "")).thenReturn("违禁词,spam");
        when(redisOperator.increment(anyString())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> governanceService.validateTextMessage(1L, "这里有违禁词内容"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("消息内容包含敏感词，请调整后再发送", exception.getMessage());
    }

    @Test
    void validateAttachmentMessageShouldRejectWhenUserRateLimitExceeded() {
        when(sysConfigService.getValueOrDefault("chat.send.rate-limit.per-minute", "30")).thenReturn("1");
        when(redisOperator.increment(anyString())).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> governanceService.validateAttachmentMessage(1L));

        assertEquals(ResultErrorCode.REQUEST_RATE_LIMITED.getCode(), exception.getCode());
        assertEquals("聊天发送过于频繁，请稍后再试", exception.getMessage());
    }

    @Test
    void validateTextMessageShouldPassWhenWithinRateLimitAndNoSensitiveWord() {
        when(sysConfigService.getValueOrDefault("chat.send.rate-limit.per-minute", "30")).thenReturn("5");
        when(sysConfigService.getValueOrDefault("chat.sensitive-words", "")).thenReturn("违禁词,spam");
        when(redisOperator.increment(anyString())).thenReturn(1L);

        assertDoesNotThrow(() -> governanceService.validateTextMessage(1L, "正常聊天内容"));

        verify(redisOperator).expire(anyString(), org.mockito.ArgumentMatchers.eq(65L), org.mockito.ArgumentMatchers.eq(java.util.concurrent.TimeUnit.SECONDS));
    }
}
