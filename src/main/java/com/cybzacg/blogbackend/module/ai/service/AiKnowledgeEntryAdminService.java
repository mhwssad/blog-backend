package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.event.ContentChangeEvent;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeEntryPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeEntryVO;

/**
 * AI 知识条目后台管理服务接口。
 *
 * <p>负责知识条目的查询、状态管理和手动刷新。
 */
public interface AiKnowledgeEntryAdminService {

    /**
     * 分页查询知识条目。
     */
    PageResult<AiKnowledgeEntryVO> listEntries(AiKnowledgeEntryPageQuery query);

    /**
     * 查询知识条目详情。
     */
    AiKnowledgeEntryVO getEntry(Long id);

    /**
     * 更新知识条目状态。
     */
    void updateEntryStatus(Long id, Integer status, Long operatorId);

    /**
     * 处理内容变更事件，更新对应知识条目状态。
     */
    void onContentChange(ContentChangeEvent event);
}
