package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageSendRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionCreateRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionDetailVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionVO;

/**
 * AI 对话服务接口。
 *
 * <p>负责会话生命周期管理、消息收发与 AI 模型交互。
 */
public interface AiChatService {

    /**
     * 创建 AI 对话会话。
     *
     * @param userId  用户ID
     * @param request 创建请求
     * @return 会话信息
     */
    AiSessionVO createSession(Long userId, AiSessionCreateRequest request);

    /**
     * 分页查询用户自己的会话列表。
     *
     * @param userId  用户ID
     * @param current 页码
     * @param size    每页条数
     * @return 分页结果
     */
    PageResult<AiSessionVO> listMySessions(Long userId, long current, long size);

    /**
     * 获取会话详情。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 会话详情
     */
    AiSessionDetailVO getSessionDetail(Long sessionId, Long userId);

    /**
     * 分页查询会话消息列表。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param current   页码
     * @param size      每页条数
     * @return 分页结果
     */
    PageResult<AiMessageVO> listMessages(Long sessionId, Long userId, long current, long size);

    /**
     * 发送消息并获取 AI 回复。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param request   消息发送请求
     * @return AI 回复消息
     */
    AiMessageVO sendMessage(Long sessionId, Long userId, AiMessageSendRequest request);

    /**
     * 关闭（删除）会话。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     */
    void deleteSession(Long sessionId, Long userId);
}
