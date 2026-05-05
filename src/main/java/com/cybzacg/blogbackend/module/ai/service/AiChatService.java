package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.user.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务接口。
 *
 * <p>负责会话生命周期管理、消息收发与 AI 模型交互。
 */
public interface AiChatService {

    AiSessionVO createSession(Long userId, AiSessionCreateRequest request);

    PageResult<AiSessionVO> listMySessions(Long userId, long current, long size);

    AiSessionDetailVO getSessionDetail(Long sessionId, Long userId);

    PageResult<AiMessageVO> listMessages(Long sessionId, Long userId, long current, long size);

    AiMessageVO sendMessage(Long sessionId, Long userId, AiMessageSendRequest request);

    /**
     * 流式发送消息，通过 SseEmitter 实时推送 AI 响应。
     */
    SseEmitter streamMessage(Long sessionId, Long userId, AiMessageSendRequest request);

    void deleteSession(Long sessionId, Long userId);
}
