package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.enums.ai.AiChatSessionStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.data.AiStreamEvent;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagRetrievalResult;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageSendRequest;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiChatMessageRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiChatSessionRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiMessageAttachmentRepository;
import com.cybzacg.blogbackend.module.ai.service.AiModelClient;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.module.ai.service.AiRagService;
import com.cybzacg.blogbackend.module.ai.service.AiTokenBudgetService;
import com.cybzacg.blogbackend.module.ai.service.AiUsageLogService;
import com.cybzacg.blogbackend.module.ai.service.impl.AiChatServiceImpl;
import com.cybzacg.blogbackend.dto.repository.file.FileInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplStreamTest {
    @Mock private AiChatSessionRepository aiChatSessionRepository;
    @Mock private AiChatMessageRepository aiChatMessageRepository;
    @Mock private AiChannelConfigRepository aiChannelConfigRepository;
    @Mock private AiModelClient aiModelClient;
    @Mock private AiQuotaService aiQuotaService;
    @Mock private AiRagService aiRagService;
    @Mock private AiUsageLogService aiUsageLogService;
    @Mock private AiModelConvert aiModelConvert;
    @Mock private AiMessageAttachmentRepository aiMessageAttachmentRepository;
    @Mock private FileInfoRepository fileInfoRepository;
    @Mock private AiTokenBudgetService aiTokenBudgetService;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatServiceImpl(
                aiChatSessionRepository, aiChatMessageRepository,
                aiChannelConfigRepository, aiModelClient,
                aiQuotaService, aiRagService, aiUsageLogService,
                aiModelConvert, aiMessageAttachmentRepository, fileInfoRepository,
                aiTokenBudgetService);
        lenient().when(aiTokenBudgetService.checkBudget(any(), any(), any(), any(), any()))
                .thenReturn(new AiTokenBudgetService.BudgetCheck(true, 0, 0, 0, 0));
    }

    @Test
    void streamMessageShouldReturnSseEmitterAndCallStreamChat() throws Exception {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("你好");

        setupSessionAndConfig(sessionId, userId);
        when(aiRagService.retrieve(any(), any())).thenReturn(new AiRagRetrievalResult());
        when(aiRagService.enrichSystemPrompt(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedContent = new AtomicReference<>();

        when(aiModelClient.streamChat(any(AiChannelConfig.class), anyString(),
                anyList(), eq("你好"), anyList(), any(Consumer.class)))
                .thenAnswer(inv -> {
                    Consumer<AiStreamEvent> consumer = inv.getArgument(5);
                    consumer.accept(AiStreamEvent.delta("你"));
                    consumer.accept(AiStreamEvent.delta("好"));
                    consumer.accept(AiStreamEvent.usage(5, 2, 7));
                    consumer.accept(AiStreamEvent.done());
                    capturedContent.set("你好");
                    latch.countDown();
                    return "你好";
                });
        when(aiChatMessageRepository.listBySessionIdOrderById(eq(sessionId), anyInt()))
                .thenReturn(Collections.emptyList());
        when(aiChatMessageRepository.save(any(AiChatMessage.class))).thenReturn(true);
        when(aiChatSessionRepository.updateById(any())).thenReturn(true);

        SseEmitter emitter = aiChatService.streamMessage(sessionId, userId, request);

        assertNotNull(emitter);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        verify(aiModelClient).streamChat(any(AiChannelConfig.class), anyString(),
                anyList(), eq("你好"), anyList(), any(Consumer.class));
        verify(aiChatMessageRepository, atLeastOnce()).save(any(AiChatMessage.class));
    }

    @Test
    void streamMessageShouldRejectWhenSessionClosed() {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("你好");

        AiChatSession session = new AiChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setChannelConfigId(10L);
        session.setStatus(AiChatSessionStatusEnum.CLOSED.getValue());
        when(aiChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(session);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiChatService.streamMessage(sessionId, userId, request));
        assertEquals(ResultErrorCode.AI_SESSION_CLOSED.getCode(), ex.getCode());
    }

    @Test
    void streamMessageShouldRejectWhenChannelDisabled() {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("你好");

        AiChatSession session = new AiChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setChannelConfigId(10L);
        session.setStatus(AiChatSessionStatusEnum.NORMAL.getValue());
        when(aiChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(session);

        AiChannelConfig config = new AiChannelConfig();
        config.setId(10L);
        config.setChannelCode("test");
        config.setStatus(0);
        when(aiChannelConfigRepository.getById(10L)).thenReturn(config);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiChatService.streamMessage(sessionId, userId, request));
        assertEquals(ResultErrorCode.AI_CHANNEL_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void streamMessageShouldRejectWhenQuotaExceeded() {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("你好");

        setupSessionAndConfig(sessionId, userId);
        doThrow(new BusinessException(ResultErrorCode.AI_QUOTA_EXCEEDED))
                .when(aiQuotaService).checkQuota(eq(userId), any(AiChannelConfig.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiChatService.streamMessage(sessionId, userId, request));
        assertEquals(ResultErrorCode.AI_QUOTA_EXCEEDED.getCode(), ex.getCode());
    }

    @Test
    void streamMessageShouldPersistAssistantMessageAfterStream() throws Exception {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("流式测试");

        setupSessionAndConfig(sessionId, userId);
        when(aiRagService.retrieve(any(), any())).thenReturn(new AiRagRetrievalResult());
        when(aiRagService.enrichSystemPrompt(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        CountDownLatch latch = new CountDownLatch(1);
        when(aiModelClient.streamChat(any(AiChannelConfig.class), anyString(),
                anyList(), eq("流式测试"), anyList(), any(Consumer.class)))
                .thenAnswer(inv -> {
                    Consumer<AiStreamEvent> consumer = inv.getArgument(5);
                    consumer.accept(AiStreamEvent.delta("回复"));
                    consumer.accept(AiStreamEvent.done());
                    latch.countDown();
                    return "回复";
                });
        when(aiChatMessageRepository.listBySessionIdOrderById(eq(sessionId), anyInt()))
                .thenReturn(Collections.emptyList());
        when(aiChatMessageRepository.save(any(AiChatMessage.class))).thenReturn(true);
        when(aiChatSessionRepository.updateById(any())).thenReturn(true);

        aiChatService.streamMessage(sessionId, userId, request);

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // 验证助手消息被保存（用户消息 + 助手消息 = 2次 save）
        verify(aiChatMessageRepository, times(2)).save(any(AiChatMessage.class));
        verify(aiQuotaService).recordUsage(userId, 10L);
        verify(aiUsageLogService).logUsage(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(),
                anyInt(), any(), anyInt(), any(), anyLong(), any());
    }

    private void setupSessionAndConfig(Long sessionId, Long userId) {
        AiChatSession session = new AiChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setChannelConfigId(10L);
        session.setStatus(AiChatSessionStatusEnum.NORMAL.getValue());
        session.setLastMessageAt(LocalDateTime.now());
        when(aiChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(session);

        AiChannelConfig config = new AiChannelConfig();
        config.setId(10L);
        config.setChannelCode("test");
        config.setStatus(1);
        config.setSystemPromptTemplate("You are helpful.");
        when(aiChannelConfigRepository.getById(10L)).thenReturn(config);
    }
}
