package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.domain.AiChannelConfig;
import com.cybzacg.blogbackend.module.ai.model.user.AiQuotaVO;

/**
 * AI 配额服务接口。
 *
 * <p>负责全局开关校验、平台/用户每日额度检查与记录、配额查询。
 */
public interface AiQuotaService {

    /**
     * 校验用户是否有权调用 AI。
     *
     * <p>依次检查：全局开关 → 平台每日额度 → 用户每日额度。
     *
     * @param userId 用户ID
     * @param config 渠道配置
     */
    void checkQuota(Long userId, AiChannelConfig config);

    /**
     * 记录一次 AI 调用，递增平台和用户维度的 Redis 计数器。
     *
     * @param userId          用户ID
     * @param channelConfigId 渠道配置ID
     */
    void recordUsage(Long userId, Long channelConfigId);

    /**
     * 查询用户当日 AI 配额使用情况。
     *
     * @param userId 用户ID
     * @param config 渠道配置
     * @return 配额信息
     */
    AiQuotaVO getUserQuota(Long userId, AiChannelConfig config);

    /**
     * 查询用户在默认渠道的当日 AI 配额使用情况。
     *
     * @param userId 用户ID
     * @return 配额信息，无可用渠道时返回空配额
     */
    AiQuotaVO getUserQuotaForDefaultChannel(Long userId);
}
