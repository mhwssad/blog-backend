package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeChunk;
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
     *
     * @param entryId 知识条目ID
     * @param chunks  待写入的知识分块列表（含向量）
     */
    void upsertChunks(Long entryId, List<AiKnowledgeChunk> chunks);

    /**
     * 删除指定知识条目的向量。
     *
     * @param entryId 知识条目ID
     */
    void deleteByEntryId(Long entryId);

    /**
     * 按查询向量检索相关分块。
     *
     * @param queryEmbedding 查询文本的向量表示
     * @param topK           返回的最大结果数
     * @param minScore       最低相似度阈值
     * @return 命中的知识分块列表
     */
    List<AiRagHit> search(List<Float> queryEmbedding, int topK, double minScore);
}
