package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;

/**
 * AI 知识条目分块服务。
 *
 * <p>负责知识条目的文本分块、向量索引重建与删除。
 */
public interface AiKnowledgeChunkService {

    /**
     * 重建指定知识条目的分块和向量索引。
     *
     * @param entry 待重建的知识条目（含标题、正文等）
     * @return 有效分块数量
     */
    int rebuildChunks(AiKnowledgeEntry entry);

    /**
     * 删除指定知识条目的分块和向量索引。
     *
     * @param entryId 知识条目ID
     */
    void deleteChunks(Long entryId);
}
