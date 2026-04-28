package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.AiUsageLog;

import java.time.LocalDateTime;

/**
 * AiUsageLog Repository。
 */
public interface AiUsageLogRepository extends IService<AiUsageLog> {

    /**
     * 统计用户在时间范围内的调用次数。
     */
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计渠道在时间范围内的调用次数。
     */
    long countByChannelConfigIdAndCreatedAtBetween(Long channelConfigId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计时间范围内指定成功状态的调用次数。
     */
    long countByCreatedAtBetweenAndSuccessStatus(LocalDateTime startTime, LocalDateTime endTime, Integer successStatus);
}
