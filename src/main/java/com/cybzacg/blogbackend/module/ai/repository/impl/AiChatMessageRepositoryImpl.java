package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.mapper.ai.AiChatMessageMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiChatMessageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AiChatMessage Repository 实现。
 */
@Repository
public class AiChatMessageRepositoryImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage>
        implements AiChatMessageRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AiChatMessage> pageBySessionId(Long sessionId, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AiChatMessage> listBySessionIdOrderById(Long sessionId, int limit) {
        int actualLimit = limit <= 0 ? 200 : limit;
        return list(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getId)
                .last("limit " + actualLimit));
    }
}
