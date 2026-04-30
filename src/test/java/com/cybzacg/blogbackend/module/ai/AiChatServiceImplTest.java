package com.cybzacg.blogbackend.module.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.enums.ai.AiChatSessionStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.ai.convert.AiModelMapper;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageSendRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionCreateRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionVO;
import com.cybzacg.blogbackend.module.ai.repository.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiChatMessageRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiChatSessionRepository;
import com.cybzacg.blogbackend.module.ai.service.AiModelClient;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.module.ai.service.AiUsageLogService;
import com.cybzacg.blogbackend.module.ai.service.impl.AiChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * AiChatServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AiChatSessionRepository aiChatSessionRepository;
    @Mock
    private AiChatMessageRepository aiChatMessageRepository;
    @Mock
    private AiChannelConfigRepository aiChannelConfigRepository;
    @Mock
    private AiModelClient aiModelClient;
    @Mock
    private AiQuotaService aiQuotaService;
    @Mock
    private AiUsageLogService aiUsageLogService;
    @Mock
    private AiModelMapper aiModelMapper;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatServiceImpl(
                aiChatSessionRepository,
                aiChatMessageRepository,
                aiChannelConfigRepository,
                aiModelClient,
                aiQuotaService,
                aiUsageLogService,
                aiModelMapper
        );
    }

    // ========== createSession ==========

    @Test
    void createSessionShouldUseDefaultChannel() {
        Long userId = 1L;
        AiSessionCreateRequest request = new AiSessionCreateRequest();
        request.setTitle("Test Session");

        AiChannelConfig defaultChannel = buildEnabledChannel(10L, "default-channel");
        defaultChannel.setIsDefault(1);
        when(aiChannelConfigRepository.listEnabledOrderByDefault())
                .thenReturn(List.of(defaultChannel));
        when(aiChatSessionRepository.save(any(AiChatSession.class))).thenReturn(true);

        AiSessionVO expectedVO = new AiSessionVO();
        expectedVO.setId(100L);
        when(aiModelMapper.toSessionVO(any(AiChatSession.class))).thenReturn(expectedVO);

        AiSessionVO result = aiChatService.createSession(userId, request);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(aiChannelConfigRepository).listEnabledOrderByDefault();
        verify(aiChannelConfigRepository, never()).getById(anyLong());

        ArgumentCaptor<AiChatSession> captor = ArgumentCaptor.forClass(AiChatSession.class);
        verify(aiChatSessionRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getChannelConfigId());
        assertEquals(userId, captor.getValue().getUserId());
    }

    // ========== sendMessage ==========

    @Test
    void sendMessageShouldSaveUserAndAssistantMessages() {
        Long sessionId = 1L;
        Long userId = 1L;
        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("Hello AI");

        AiChatSession session = buildNormalSession(sessionId, userId, 10L);
        AiChannelConfig config = buildEnabledChannel(10L, "test-channel");

        when(aiChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(session);
        when(aiChannelConfigRepository.getById(10L)).thenReturn(config);

        AiModelCallResult callResult = new AiModelCallResult();
        callResult.setContent("Hello human");
        callResult.setSuccess(true);
        callResult.setRequestTokens(10);
        callResult.setResponseTokens(20);
        callResult.setTotalTokens(30);
        when(aiModelClient.chat(any(), any(), any(), eq("Hello AI"))).thenReturn(callResult);
        when(aiChatMessageRepository.listBySessionIdOrderById(eq(sessionId), anyInt()))
                .thenReturn(Collections.emptyList());
        when(aiChatMessageRepository.save(any(AiChatMessage.class))).thenReturn(true);
        when(aiChatSessionRepository.updateById(any(AiChatSession.class))).thenReturn(true);

        AiMessageVO expectedVO = new AiMessageVO();
        expectedVO.setContent("Hello human");
        when(aiModelMapper.toMessageVO(any(AiChatMessage.class))).thenReturn(expectedVO);

        AiMessageVO result = aiChatService.sendMessage(sessionId, userId, request);

        assertNotNull(result);
        assertEquals("Hello human", result.getContent());
        verify(aiChatMessageRepository, times(2)).save(any(AiChatMessage.class));
        verify(aiQuotaService).recordUsage(userId, config.getId());
        verify(aiUsageLogService).logUsage(eq(userId), eq(10L), eq(sessionId),
                any(), eq(10), eq(20), eq(30), any(), any());
    }

    @Test
    void sendMessageShouldHandleModelCallFailure() {
        Long sessionId = 1L;
        Long userId = 1L;
        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("Hello AI");

        AiChatSession session = buildNormalSession(sessionId, userId, 10L);
        AiChannelConfig config = buildEnabledChannel(10L, "test-channel");

        when(aiChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(session);
        when(aiChannelConfigRepository.getById(10L)).thenReturn(config);
        when(aiModelClient.chat(any(), any(), any(), eq("Hello AI")))
                .thenThrow(new RuntimeException("Model unavailable"));
        when(aiChatMessageRepository.listBySessionIdOrderById(eq(sessionId), anyInt()))
                .thenReturn(Collections.emptyList());
        when(aiChatMessageRepository.save(any(AiChatMessage.class))).thenReturn(true);
        when(aiChatSessionRepository.updateById(any(AiChatSession.class))).thenReturn(true);
        when(aiModelMapper.toMessageVO(any(AiChatMessage.class))).thenReturn(new AiMessageVO());

        AiMessageVO result = aiChatService.sendMessage(sessionId, userId, request);

        assertNotNull(result);
        ArgumentCaptor<AiChatMessage> messageCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
        verify(aiChatMessageRepository, times(2)).save(messageCaptor.capture());
        List<AiChatMessage> savedMessages = messageCaptor.getAllValues();
        // Second message is the assistant message with error
        AiChatMessage assistantMessage = savedMessages.get(1);
        assertEquals(0, assistantMessage.getResponseStatus()); // FAILED status
        assertEquals("Model unavailable", assistantMessage.getErrorMessage());
    }

    // ========== deleteSession ==========

    @Test
    void deleteSessionShouldSetClosedStatus() {
        Long sessionId = 1L;
        Long userId = 1L;

        AiChatSession session = buildNormalSession(sessionId, userId, 10L);
        when(aiChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(session);
        when(aiChatSessionRepository.updateById(any(AiChatSession.class))).thenReturn(true);

        aiChatService.deleteSession(sessionId, userId);

        ArgumentCaptor<AiChatSession> captor = ArgumentCaptor.forClass(AiChatSession.class);
        verify(aiChatSessionRepository).updateById(captor.capture());
        assertEquals(AiChatSessionStatusEnum.CLOSED.getValue(), captor.getValue().getStatus());
    }

    // ========== sendMessage quota exceeded ==========

    @Test
    void sendMessageShouldRejectWhenQuotaExceeded() {
        Long sessionId = 1L;
        Long userId = 1L;
        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("Hello AI");

        AiChatSession session = buildNormalSession(sessionId, userId, 10L);
        AiChannelConfig config = buildEnabledChannel(10L, "test-channel");

        when(aiChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(session);
        when(aiChannelConfigRepository.getById(10L)).thenReturn(config);
        doThrow(new BusinessException(ResultErrorCode.AI_QUOTA_EXCEEDED))
                .when(aiQuotaService).checkQuota(userId, config);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiChatService.sendMessage(sessionId, userId, request));

        assertEquals(ResultErrorCode.AI_QUOTA_EXCEEDED.getCode(), exception.getCode());
        verify(aiChatMessageRepository, never()).save(any());
    }

    // ========== listMySessions ==========

    @Test
    void listMySessionsShouldReturnOnlyOwnedSessions() {
        Long userId = 1L;

        Page<AiChatSession> page = new Page<>(1, 10);
        AiChatSession session1 = buildNormalSession(1L, userId, 10L);
        page.setRecords(List.of(session1));
        page.setTotal(1);

        when(aiChatSessionRepository.pageByUserIdAndStatus(
                eq(userId), eq(AiChatSessionStatusEnum.NORMAL.getValue()), eq(1L), eq(10L)))
                .thenReturn(page);
        when(aiModelMapper.toSessionVO(any(AiChatSession.class))).thenReturn(new AiSessionVO());

        PageResult<AiSessionVO> result = aiChatService.listMySessions(userId, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(aiChatSessionRepository).pageByUserIdAndStatus(
                eq(userId), eq(AiChatSessionStatusEnum.NORMAL.getValue()), eq(1L), eq(10L));
    }

    // ========== Helper methods ==========

    private AiChannelConfig buildEnabledChannel(Long id, String channelCode) {
        AiChannelConfig config = new AiChannelConfig();
        config.setId(id);
        config.setChannelCode(channelCode);
        config.setStatus(1);
        config.setSystemPromptTemplate("You are a helpful assistant.");
        return config;
    }

    private AiChatSession buildNormalSession(Long sessionId, Long userId, Long channelConfigId) {
        AiChatSession session = new AiChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setChannelConfigId(channelConfigId);
        session.setStatus(AiChatSessionStatusEnum.NORMAL.getValue());
        session.setLastMessageAt(LocalDateTime.now());
        return session;
    }
}
