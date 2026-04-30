package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.mapper.ai.AiChatSessionMapper;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionPageQuery;
import com.cybzacg.blogbackend.module.ai.repository.AiChatSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AiChatSession Repository 实现。
 */
@Repository
public class AiChatSessionRepositoryImpl extends ServiceImpl<AiChatSessionMapper, AiChatSession>
        implements AiChatSessionRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public AiChatSession findByIdAndUserId(Long sessionId, Long userId) {
        return getOne(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getId, sessionId)
                .eq(AiChatSession::getUserId, userId)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AiChatSession> listByUserIdAndStatusOrderByUpdatedAt(Long userId, Integer status, int limit) {
        int actualLimit = limit <= 0 ? 50 : limit;
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .eq(status != null, AiChatSession::getStatus, status)
                .orderByDesc(AiChatSession::getUpdatedAt)
                .orderByDesc(AiChatSession::getId)
                .last("limit " + actualLimit);
        return list(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AiChatSession> pageByUserIdAndStatus(Long userId, Integer status, long current, long size) {
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .eq(status != null, AiChatSession::getStatus, status)
                .orderByDesc(AiChatSession::getUpdatedAt)
                .orderByDesc(AiChatSession::getId);
        return page(new Page<>(current, size), wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AiChatSession> pageByAdminConditions(AiSessionPageQuery query) {
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<AiChatSession>()
                .eq(query.getUserId() != null, AiChatSession::getUserId, query.getUserId())
                .eq(query.getStatus() != null, AiChatSession::getStatus, query.getStatus())
                .eq(query.getChannelConfigId() != null, AiChatSession::getChannelConfigId, query.getChannelConfigId())
                .ge(query.getStartTime() != null, AiChatSession::getCreatedAt, query.getStartTime())
                .le(query.getEndTime() != null, AiChatSession::getCreatedAt, query.getEndTime())
                .orderByDesc(AiChatSession::getUpdatedAt)
                .orderByDesc(AiChatSession::getId);
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }
}
