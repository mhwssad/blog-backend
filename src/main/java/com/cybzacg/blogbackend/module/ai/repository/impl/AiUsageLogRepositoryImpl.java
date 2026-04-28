package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.AiUsageLog;
import com.cybzacg.blogbackend.mapper.AiUsageLogMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiUsageLogRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * AiUsageLog Repository 实现。
 */
@Repository
public class AiUsageLogRepositoryImpl extends ServiceImpl<AiUsageLogMapper, AiUsageLog>
        implements AiUsageLogRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return count(new LambdaQueryWrapper<AiUsageLog>()
                .eq(AiUsageLog::getUserId, userId)
                .ge(startTime != null, AiUsageLog::getCreatedAt, startTime)
                .le(endTime != null, AiUsageLog::getCreatedAt, endTime));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByChannelConfigIdAndCreatedAtBetween(Long channelConfigId, LocalDateTime startTime, LocalDateTime endTime) {
        return count(new LambdaQueryWrapper<AiUsageLog>()
                .eq(AiUsageLog::getChannelConfigId, channelConfigId)
                .ge(startTime != null, AiUsageLog::getCreatedAt, startTime)
                .le(endTime != null, AiUsageLog::getCreatedAt, endTime));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByCreatedAtBetweenAndSuccessStatus(LocalDateTime startTime, LocalDateTime endTime, Integer successStatus) {
        return count(new LambdaQueryWrapper<AiUsageLog>()
                .eq(successStatus != null, AiUsageLog::getSuccessStatus, successStatus)
                .ge(startTime != null, AiUsageLog::getCreatedAt, startTime)
                .le(endTime != null, AiUsageLog::getCreatedAt, endTime));
    }
}
