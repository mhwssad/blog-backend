package com.cybzacg.blogbackend.dto.repository.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.dto.mapper.ai.AiChatMessageMapper;
import com.cybzacg.blogbackend.dto.repository.ai.AiChatMessageRepository;
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
