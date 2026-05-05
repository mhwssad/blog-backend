package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagHit;

import java.util.List;

/**
 * AI 向量存储抽象。
 *
 * <p>当前默认实现使用 MySQL 持久化和 Redis 缓存，后续可替换为 Milvus、Qdrant 或 Elasticsearch。
 */
public interface AiVectorStore {

    /**
     * 写入知识分块向量。
     */
    void upsertChunks(Long entryId, List<AiKnowledgeChunk> chunks);

    /**
     * 删除指定知识条目的向量。
     */
    void deleteByEntryId(Long entryId);

    /**
     * 按查询向量检索相关分块。
     */
    List<AiRagHit> search(List<Float> queryEmbedding, int topK, double minScore);
}
