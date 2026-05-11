package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.dto.domain.ai.AiChannelAccount;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;

/**
 * AI 账号池服务。
 *
 * <p>负责从渠道的账号池中按额度选号、健康追踪和自动恢复。
 */
public interface AiAccountPoolService {

    /**
     * 从池中选择一个有额度且健康的账号，无可用时返回 null。
     */
    AiChannelAccount selectAccount(AiChannelConfig config);

    /**
     * 报告调用成功，重置错误计数并递增用量。
     */
    void reportSuccess(Long accountId);

    /**
     * 报告调用失败，递增错误计数，超阈值自动禁用。
     */
    void reportFailure(Long accountId, String errorMessage);

    /**
     * 恢复到期禁用账号。
     */
    void recoverDisabledAccounts();

    /**
     * 检查账号今日用量。
     */
    long getTodayUsage(Long accountId);

    /**
     * 递增账号今日用量。
     */
    void incrementTodayUsage(Long accountId);
}
