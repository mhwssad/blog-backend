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

    /**
     * 创建新的 AI 对话会话。
     *
     * @param userId  当前登录用户ID
     * @param request 会话创建请求（含标题、关联 Agent 等）
     * @return 创建后的会话视图对象
     */
    AiSessionVO createSession(Long userId, AiSessionCreateRequest request);

    /**
     * 分页查询当前用户的 AI 会话列表。
     *
     * @param userId  当前登录用户ID
     * @param current 页码
     * @param size    每页条数
     * @return 分页结果
     */
    PageResult<AiSessionVO> listMySessions(Long userId, long current, long size);

    /**
     * 查询会话详情。
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID（用于权限校验）
     * @return 会话详情视图对象
     */
    AiSessionDetailVO getSessionDetail(Long sessionId, Long userId);

    /**
     * 分页查询会话内的消息列表。
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID（用于权限校验）
     * @param current   页码
     * @param size      每页条数
     * @return 分页结果
     */
    PageResult<AiMessageVO> listMessages(Long sessionId, Long userId, long current, long size);

    /**
     * 发送消息并同步等待 AI 响应。
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID（用于权限校验）
     * @param request   消息发送请求（含内容、附件等）
     * @return AI 响应消息视图对象
     */
    AiMessageVO sendMessage(Long sessionId, Long userId, AiMessageSendRequest request);

    /**
     * 流式发送消息，通过 SseEmitter 实时推送 AI 响应。
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID（用于权限校验）
     * @param request   消息发送请求（含内容、附件等）
     * @return SSE 推送发射器
     */
    SseEmitter streamMessage(Long sessionId, Long userId, AiMessageSendRequest request);

    /**
     * 删除会话。
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID（用于权限校验）
     */
    void deleteSession(Long sessionId, Long userId);
}
