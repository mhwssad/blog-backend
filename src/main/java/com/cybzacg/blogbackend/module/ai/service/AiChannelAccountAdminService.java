package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountVO;

/**
 * AI 渠道账号池后台管理服务。
 */
public interface AiChannelAccountAdminService {

    PageResult<AiChannelAccountVO> listAccounts(Long channelConfigId, long current, long size);

    AiChannelAccountVO getAccount(Long channelConfigId, Long accountId);

    AiChannelAccountVO createAccount(Long channelConfigId, AiChannelAccountSaveRequest request, Long operatorId);

    AiChannelAccountVO updateAccount(Long channelConfigId, Long accountId, AiChannelAccountSaveRequest request, Long operatorId);

    void updateAccountStatus(Long channelConfigId, Long accountId, Integer status, Long operatorId);

    void deleteAccount(Long channelConfigId, Long accountId, Long operatorId);
}
