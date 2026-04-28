package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 按条件分页查询使用日志。
     *
     * @param userId          用户ID（可为 null）
     * @param channelConfigId 渠道配置ID（可为 null）
     * @param startTime       开始时间（可为 null）
     * @param endTime         结束时间（可为 null）
     * @param successStatus   成功状态（可为 null）
     * @param current         页码
     * @param size            每页条数
     * @return 分页结果
     */
    Page<AiUsageLog> pageByQuery(Long userId, Long channelConfigId,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  Integer successStatus, long current, long size);

    /**
     * 统计时间范围内的总 token 数。
     *
     * @param startTime 开始时间（可为 null）
     * @param endTime   结束时间（可为 null）
     * @return 总 token 数之和
     */
    long sumTotalTokensByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计时间范围内的总额度消耗。
     *
     * @param startTime 开始时间（可为 null）
     * @param endTime   结束时间（可为 null）
     * @return 总额度消耗之和
     */
    long sumQuotaCostByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}
