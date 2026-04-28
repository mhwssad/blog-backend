package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.AiChatSession;

import java.util.List;

/**
 * AiChatSession Repository。
 */
public interface AiChatSessionRepository extends IService<AiChatSession> {

    /**
     * 查询用户自己的 AI 会话。
     */
    AiChatSession findByIdAndUserId(Long sessionId, Long userId);

    /**
     * 按用户、状态读取最近会话。
     */
    List<AiChatSession> listByUserIdAndStatusOrderByUpdatedAt(Long userId, Integer status, int limit);
}
