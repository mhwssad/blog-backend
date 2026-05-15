package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.config.property.AiRagProperties;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeChunkRepository;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagHit;
import com.cybzacg.blogbackend.module.ai.service.AiVectorStore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * MySQL + Redis 向量存储实现。
 *
 * <p>MySQL 作为权威存储，Redis 缓存活跃分块列表；缓存失效时回源 MySQL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MysqlRedisAiVectorStore implements AiVectorStore {
    private static final int DEFAULT_SEARCH_SCAN_LIMIT = 5000;

    private final AiKnowledgeChunkRepository aiKnowledgeChunkRepository;
    private final RedisOperator redisOperator;
    private final AiRagProperties ragProperties;

    /**
     * 替换写入指定知识条目的分块：先将该条目已有分块标记为停用，再批量保存新分块，最后清除缓存。
     *
     * @param entryId 知识条目 ID
     * @param chunks  新的分块列表，为空时仅停用旧分块
     */
    @Override
    public void upsertChunks(Long entryId, List<AiKnowledgeChunk> chunks) {
        aiKnowledgeChunkRepository.disableByEntryId(entryId);
        if (chunks != null && !chunks.isEmpty()) {
            log.debug("写入知识分块: entryId={}, chunkCount={}", entryId, chunks.size());
            aiKnowledgeChunkRepository.saveBatch(chunks);
        }
        invalidateCache();
    }

    /**
     * 按 entryId 软删除分块并清除缓存。
     *
     * @param entryId 知识条目 ID
     */
    @Override
    public void deleteByEntryId(Long entryId) {
        aiKnowledgeChunkRepository.disableByEntryId(entryId);
        invalidateCache();
    }

    /**
     * 基于余弦相似度的向量检索：加载全部活跃分块后逐条计算相似度，过滤并排序返回 TopK 结果。
     *
     * @param queryEmbedding 查询向量
     * @param topK           返回结果数量上限，0 或负数时使用配置默认值
     * @param minScore       最低相似度阈值
     * @return 命中的知识分块列表，按相似度降序排列
     */
    @Override
    public List<AiRagHit> search(List<Float> queryEmbedding, int topK, double minScore) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return List.of();
        }
        int actualTopK = topK <= 0 ? ragProperties.getTopK() : topK;
        return loadActiveChunks().stream()
                .map(chunk -> AiRagHit.builder()
                        .chunk(chunk)
                        .score(cosineSimilarity(queryEmbedding, parseEmbedding(chunk.getEmbeddingJson())))
                        .build())
                .filter(hit -> hit.getScore() >= minScore)
                .sorted(Comparator.comparingDouble(AiRagHit::getScore).reversed())
                .limit(actualTopK)
                .toList();
    }

    /**
     * 加载活跃分块列表，优先从 Redis 缓存读取，缓存未命中或异常时回源 MySQL。
     * 回源后尝试写入缓存，写入失败不影响检索主链路。
     *
     * @return 活跃分块列表
     */
    private List<AiKnowledgeChunk> loadActiveChunks() {
        try {
            List<AiKnowledgeChunk> cached = redisOperator.get(
                    RedisConstants.AI_RAG_ACTIVE_CHUNKS_CACHE_KEY,
                    new TypeReference<>() {
                    });
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        } catch (RuntimeException ex) {
            // Redis 缓存不可用时回源 MySQL，保证 RAG 仍可工作
            log.warn("Redis 缓存读取异常，回源 MySQL 加载活跃分块: {}", ex.getMessage());
        }
        List<AiKnowledgeChunk> chunks = aiKnowledgeChunkRepository.listActiveForSearch(DEFAULT_SEARCH_SCAN_LIMIT);
        try {
            redisOperator.set(RedisConstants.AI_RAG_ACTIVE_CHUNKS_CACHE_KEY, chunks, ragProperties.getCacheTtl());
        } catch (RuntimeException ex) {
            // 缓存写入失败不影响检索主链路
            log.warn("Redis 缓存写入活跃分块失败: {}", ex.getMessage());
        }
        return chunks;
    }

    /**
     * 删除活跃分块缓存 Key，下次检索时将重新从 MySQL 加载并缓存。
     */
    private void invalidateCache() {
        redisOperator.delete(RedisConstants.AI_RAG_ACTIVE_CHUNKS_CACHE_KEY);
    }

    /**
     * 将 JSON 字符串解析为浮点向量列表，解析失败时返回空列表以保证检索不中断。
     *
     * @param embeddingJson 向量的 JSON 数组字符串
     * @return 解析后的浮点列表
     */
    private List<Float> parseEmbedding(String embeddingJson) {
        try {
            List<Float> embedding = JsonUtils.fromJson(embeddingJson, new TypeReference<>() {
            });
            return embedding == null ? List.of() : embedding;
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    /**
     * 计算两个向量的余弦相似度。当维度不匹配时取较短向量的长度进行计算。
     *
     * @param left  左侧向量
     * @param right 右侧向量
     * @return 余弦相似度，范围 [0, 1]；任一向量模为零时返回 0
     */
    private double cosineSimilarity(List<Float> left, List<Float> right) {
        int size = Math.min(left.size(), right.size());
        if (size == 0) {
            return 0D;
        }
        // 计算点积与各自 L2 范数的平方
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int i = 0; i < size; i++) {
            double lv = left.get(i);
            double rv = right.get(i);
            dot += lv * rv;
            leftNorm += lv * lv;
            rightNorm += rv * rv;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
