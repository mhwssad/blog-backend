package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigVO;

import java.util.List;

/**
 * AI 知识源配置后台管理服务接口。
 *
 * <p>负责知识源配置的查询、更新、启停管理。
 */
public interface AiKnowledgeSourceConfigAdminService {

    /**
     * 查询所有知识源配置列表。
     */
    List<AiKnowledgeSourceConfigVO> listConfigs();

    /**
     * 查询知识源配置详情。
     */
    AiKnowledgeSourceConfigVO getConfig(Long id);

    /**
     * 更新知识源配置。
     */
    AiKnowledgeSourceConfigVO updateConfig(Long id, AiKnowledgeSourceConfigSaveRequest request, Long operatorId);

    /**
     * 切换知识源启停状态。
     */
    void toggleEnabled(Long id, Integer enabled, Long operatorId);

    /**
     * 初始化默认知识源配置（首次启动时调用）。
     */
    void initDefaultConfigs();
}
