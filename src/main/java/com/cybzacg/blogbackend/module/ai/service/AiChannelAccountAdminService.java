package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountVO;

/**
 * AI 渠道账号池后台管理服务。
 *
 * <p>负责渠道下 API 账号的增删改查及状态管理，每个渠道可配置多个账号以实现负载均衡与额度隔离。
 */
public interface AiChannelAccountAdminService {

    /**
     * 分页查询指定渠道下的账号列表。
     *
     * @param channelConfigId 渠道配置ID
     * @param current         页码
     * @param size            每页条数
     * @return 分页结果
     */
    PageResult<AiChannelAccountVO> listAccounts(Long channelConfigId, long current, long size);

    /**
     * 查询指定渠道下的账号详情。
     *
     * @param channelConfigId 渠道配置ID
     * @param accountId       账号ID
     * @return 账号详情视图对象
     */
    AiChannelAccountVO getAccount(Long channelConfigId, Long accountId);

    /**
     * 在指定渠道下创建新账号。
     *
     * @param channelConfigId 渠道配置ID
     * @param request         账号创建请求（含 API Key、额度等）
     * @param operatorId      操作人ID
     * @return 创建后的账号视图对象
     */
    AiChannelAccountVO createAccount(Long channelConfigId, AiChannelAccountSaveRequest request, Long operatorId);

    /**
     * 更新指定渠道下的账号配置。
     *
     * @param channelConfigId 渠道配置ID
     * @param accountId       账号ID
     * @param request         更新请求
     * @param operatorId      操作人ID
     * @return 更新后的账号视图对象
     */
    AiChannelAccountVO updateAccount(Long channelConfigId, Long accountId, AiChannelAccountSaveRequest request, Long operatorId);

    /**
     * 更新账号状态（启用/禁用）。
     *
     * @param channelConfigId 渠道配置ID
     * @param accountId       账号ID
     * @param status          目标状态
     * @param operatorId      操作人ID
     */
    void updateAccountStatus(Long channelConfigId, Long accountId, Integer status, Long operatorId);

    /**
     * 删除指定渠道下的账号。
     *
     * @param channelConfigId 渠道配置ID
     * @param accountId       账号ID
     * @param operatorId      操作人ID
     */
    void deleteAccount(Long channelConfigId, Long accountId, Long operatorId);
}
