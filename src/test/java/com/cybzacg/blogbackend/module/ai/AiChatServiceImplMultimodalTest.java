package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.dto.domain.ai.AiMessageAttachment;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.ai.AiChatSessionStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagRetrievalResult;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageSendRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageVO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplMultimodalTest {
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
        lenient().when(aiRagService.retrieve(any(), any())).thenReturn(new AiRagRetrievalResult());
        lenient().when(aiRagService.enrichSystemPrompt(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(aiTokenBudgetService.checkBudget(any(), any(), any(), any(), any()))
                .thenReturn(new AiTokenBudgetService.BudgetCheck(true, 0, 0, 0, 0));
    }

    @Test
    void sendMessageWithImageAttachmentsShouldResolveAndSaveAttachments() {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("描述这张图片");
        request.setAttachmentFileIds(List.of(100L, 101L));

        FileInfo img1 = buildImageFile(100L, userId, "image/png");
        FileInfo img2 = buildImageFile(101L, userId, "image/jpeg");

        setupSessionAndConfig(sessionId, userId);
        when(fileInfoRepository.listByIds(List.of(100L, 101L))).thenReturn(List.of(img1, img2));
        when(aiMessageAttachmentRepository.saveBatch(anyList())).thenReturn(true);

        AiModelCallResult callResult = new AiModelCallResult();
        callResult.setSuccess(true);
        callResult.setContent("这是一张图片描述");
        callResult.setRequestTokens(50);
        callResult.setResponseTokens(100);
        callResult.setTotalTokens(150);
        when(aiModelClient.chat(any(), any(), any(), eq("描述这张图片"), any())).thenReturn(callResult);
        when(aiChatMessageRepository.listBySessionIdOrderById(eq(sessionId), anyInt()))
                .thenReturn(Collections.emptyList());
        when(aiChatMessageRepository.save(any(AiChatMessage.class))).thenReturn(true);
        when(aiChatSessionRepository.updateById(any())).thenReturn(true);
        when(aiModelConvert.toMessageVO(any(AiChatMessage.class))).thenReturn(new AiMessageVO());
        when(aiMessageAttachmentRepository.listByMessageIds(anyList())).thenReturn(List.of());

        AiMessageVO result = aiChatService.sendMessage(sessionId, userId, request);
        assertNotNull(result);

        // 验证附件关联被保存
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiMessageAttachment>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiMessageAttachmentRepository).saveBatch(captor.capture());
        assertEquals(2, captor.getValue().size());

        // 验证模型调用接收了图片附件
        ArgumentCaptor<List<FileInfo>> imgCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiModelClient).chat(any(), any(), any(), eq("描述这张图片"), imgCaptor.capture());
        assertEquals(2, imgCaptor.getValue().size());
    }

    @Test
    void sendMessageShouldRejectUnownedFile() {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("描述图片");
        request.setAttachmentFileIds(List.of(100L));

        // 文件属于其他用户
        FileInfo othersFile = buildImageFile(100L, 999L, "image/png");

        setupSessionAndConfig(sessionId, userId);
        when(fileInfoRepository.listByIds(List.of(100L))).thenReturn(List.of(othersFile));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiChatService.sendMessage(sessionId, userId, request));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
    }

    @Test
    void sendMessageShouldRejectUnsupportedFileType() {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("分析文档");
        request.setAttachmentFileIds(List.of(200L));

        FileInfo doc = buildImageFile(200L, userId, "application/pdf");
        doc.setFileType("document");

        setupSessionAndConfig(sessionId, userId);
        when(fileInfoRepository.listByIds(List.of(200L))).thenReturn(List.of(doc));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiChatService.sendMessage(sessionId, userId, request));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
    }

    @Test
    void sendMessageShouldWorkWithoutAttachments() {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("你好");

        setupSessionAndConfig(sessionId, userId);

        AiModelCallResult callResult = new AiModelCallResult();
        callResult.setSuccess(true);
        callResult.setContent("你好！");
        callResult.setRequestTokens(5);
        callResult.setResponseTokens(10);
        callResult.setTotalTokens(15);
        when(aiModelClient.chat(any(), any(), any(), eq("你好"), any())).thenReturn(callResult);
        when(aiChatMessageRepository.listBySessionIdOrderById(eq(sessionId), anyInt()))
                .thenReturn(Collections.emptyList());
        when(aiChatMessageRepository.save(any(AiChatMessage.class))).thenReturn(true);
        when(aiChatSessionRepository.updateById(any())).thenReturn(true);
        when(aiModelConvert.toMessageVO(any(AiChatMessage.class))).thenReturn(new AiMessageVO());
        when(aiMessageAttachmentRepository.listByMessageIds(anyList())).thenReturn(List.of());

        AiMessageVO result = aiChatService.sendMessage(sessionId, userId, request);
        assertNotNull(result);

        verify(fileInfoRepository, never()).listByIds(anyList());
        verify(aiMessageAttachmentRepository, never()).saveBatch(anyList());

        // 验证模型调用附件参数为空列表
        ArgumentCaptor<List<FileInfo>> imgCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiModelClient).chat(any(), any(), any(), eq("你好"), imgCaptor.capture());
        assertTrue(imgCaptor.getValue().isEmpty());
    }

    @Test
    void sendMessageShouldRejectDeletedFile() {
        Long userId = 7L;
        Long sessionId = 1L;

        AiMessageSendRequest request = new AiMessageSendRequest();
        request.setContent("描述图片");
        request.setAttachmentFileIds(List.of(300L));

        FileInfo deleted = buildImageFile(300L, userId, "image/png");
        deleted.setStatus(0);

        setupSessionAndConfig(sessionId, userId);
        when(fileInfoRepository.listByIds(List.of(300L))).thenReturn(List.of(deleted));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiChatService.sendMessage(sessionId, userId, request));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
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

    private FileInfo buildImageFile(Long id, Long userId, String mimeType) {
        FileInfo fi = new FileInfo();
        fi.setId(id);
        fi.setUploadUserId(userId);
        fi.setFileType("image");
        fi.setMimeType(mimeType);
        fi.setStatus(1);
        fi.setFileUrl("https://example.com/file/" + id);
        fi.setFilePath("/uploads/file/" + id);
        return fi;
    }
}
