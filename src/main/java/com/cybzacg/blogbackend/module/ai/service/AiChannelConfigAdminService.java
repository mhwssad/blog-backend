package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigVO;

/**
 * AI 渠道配置后台管理服务接口。
 *
 * <p>负责渠道配置的增删改查及状态管理。
 */
public interface AiChannelConfigAdminService {

    /**
     * 分页查询渠道配置列表。
     *
     * @param current 页码
     * @param size    每页条数
     * @return 分页结果
     */
    PageResult<AiChannelConfigVO> listChannels(long current, long size);

    /**
     * 查询渠道配置详情。
     *
     * @param id 渠道配置ID
     * @return 渠道配置信息
     */
    AiChannelConfigVO getChannel(Long id);

    /**
     * 创建渠道配置。
     *
     * @param request   创建请求
     * @param operatorId 操作人ID
     * @return 创建后的渠道配置信息
     */
    AiChannelConfigVO createChannel(AiChannelConfigSaveRequest request, Long operatorId);

    /**
     * 更新渠道配置。
     *
     * @param id        渠道配置ID
     * @param request   更新请求
     * @param operatorId 操作人ID
     * @return 更新后的渠道配置信息
     */
    AiChannelConfigVO updateChannel(Long id, AiChannelConfigSaveRequest request, Long operatorId);

    /**
     * 更新渠道配置状态。
     *
     * @param id        渠道配置ID
     * @param status    目标状态
     * @param operatorId 操作人ID
     */
    void updateStatus(Long id, Integer status, Long operatorId);

    /**
     * 删除渠道配置（软删除）。
     *
     * @param id        渠道配置ID
     * @param operatorId 操作人ID
     */
    void deleteChannel(Long id, Long operatorId);
}
