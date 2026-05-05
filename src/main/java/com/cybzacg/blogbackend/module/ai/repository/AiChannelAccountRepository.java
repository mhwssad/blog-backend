package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ai.AiChannelAccount;

import java.util.List;

/**
 * AI 渠道账号池数据访问。
 */
public interface AiChannelAccountRepository extends IService<AiChannelAccount> {

    /**
     * 查询渠道下所有启用账号，按权重排序。
     */
    List<AiChannelAccount> listEnabledByChannelId(Long channelConfigId);

    /**
     * 调用成功后重置连续错误计数。
     */
    boolean resetErrors(Long id);

    /**
     * 调用失败后递增连续错误计数，超阈值自动禁用。
     */
    boolean incrementErrors(Long id, String errorMessage);

    /**
     * 批量恢复到期禁用账号，返回恢复数量。
     */
    int recoverDisabledAccounts();
}
