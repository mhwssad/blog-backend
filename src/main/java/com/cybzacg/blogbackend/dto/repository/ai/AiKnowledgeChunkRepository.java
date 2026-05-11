package com.cybzacg.blogbackend.dto.repository.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeChunk;

import java.util.List;

/**
 * AiKnowledgeChunk Repository。
 */
public interface AiKnowledgeChunkRepository extends IService<AiKnowledgeChunk> {

    /**
     * 查询指定条目的有效分块。
     */
    List<AiKnowledgeChunk> listActiveByEntryId(Long entryId);

    /**
     * 查询全部有效分块，用于 v1 MySQL 余弦检索。
     */
    List<AiKnowledgeChunk> listActiveForSearch(int limit);

    /**
     * 将指定知识条目的历史分块置为失效。
     */
    void disableByEntryId(Long entryId);

    /**
     * 统计指定知识条目的有效分块数量。
     */
    long countActiveByEntryId(Long entryId);
}
