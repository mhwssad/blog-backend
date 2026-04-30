package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;

import java.util.List;

/**
 * AiChatMessage Repository。
 */
public interface AiChatMessageRepository extends IService<AiChatMessage> {

    /**
     * 按会话分页读取消息。
     */
    Page<AiChatMessage> pageBySessionId(Long sessionId, long current, long size);

    /**
     * 按会话顺序读取消息。
     */
    List<AiChatMessage> listBySessionIdOrderById(Long sessionId, int limit);
}
