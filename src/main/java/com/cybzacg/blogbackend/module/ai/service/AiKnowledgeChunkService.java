package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;

/**
 * AI 知识条目分块服务。
 */
public interface AiKnowledgeChunkService {

    /**
     * 重建指定知识条目的分块和向量索引。
     *
     * @return 有效分块数量
     */
    int rebuildChunks(AiKnowledgeEntry entry);

    /**
     * 删除指定知识条目的分块和向量索引。
     */
    void deleteChunks(Long entryId);
}
