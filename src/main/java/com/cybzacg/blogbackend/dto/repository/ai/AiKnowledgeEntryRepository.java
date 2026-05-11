package com.cybzacg.blogbackend.dto.repository.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;

import java.util.List;

/**
 * AiKnowledgeEntry Repository。
 */
public interface AiKnowledgeEntryRepository extends IService<AiKnowledgeEntry> {

    /**
     * 根据来源类型和来源ID查询条目。
     */
    AiKnowledgeEntry findBySource(String sourceType, Long sourceId);

    /**
     * 按条件分页查询知识条目。
     *
     * @param sourceType 来源类型（可为 null）
     * @param status     状态（可为 null）
     * @param keyword    标题关键词（可为 null）
     * @param current    页码
     * @param size       每页条数
     * @return 分页结果
     */
    Page<AiKnowledgeEntry> pageByQuery(String sourceType, Integer status,
                                        String keyword, long current, long size);

    /**
     * 统计指定来源类型和状态的条目数。
     */
    long countBySourceTypeAndStatus(String sourceType, Integer status);

    /**
     * 查询指定来源类型下需要同步的条目。
     */
    List<AiKnowledgeEntry> listSyncCandidates(String sourceType, String taskType, int limit);

    /**
     * 查询指定来源类型下的有效条目。
     */
    List<AiKnowledgeEntry> listActiveBySourceType(String sourceType, int limit);
}
