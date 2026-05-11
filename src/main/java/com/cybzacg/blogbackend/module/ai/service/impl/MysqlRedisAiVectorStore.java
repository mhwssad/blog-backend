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
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * MySQL + Redis 向量存储实现。
 *
 * <p>MySQL 作为权威存储，Redis 缓存活跃分块列表；缓存失效时回源 MySQL。
 */
@Service
@RequiredArgsConstructor
public class MysqlRedisAiVectorStore implements AiVectorStore {
    private static final int DEFAULT_SEARCH_SCAN_LIMIT = 5000;

    private final AiKnowledgeChunkRepository aiKnowledgeChunkRepository;
    private final RedisOperator redisOperator;
    private final AiRagProperties ragProperties;

    @Override
    public void upsertChunks(Long entryId, List<AiKnowledgeChunk> chunks) {
        aiKnowledgeChunkRepository.disableByEntryId(entryId);
        if (chunks != null && !chunks.isEmpty()) {
            aiKnowledgeChunkRepository.saveBatch(chunks);
        }
        invalidateCache();
    }

    @Override
    public void deleteByEntryId(Long entryId) {
        aiKnowledgeChunkRepository.disableByEntryId(entryId);
        invalidateCache();
    }

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
        }
        List<AiKnowledgeChunk> chunks = aiKnowledgeChunkRepository.listActiveForSearch(DEFAULT_SEARCH_SCAN_LIMIT);
        try {
            redisOperator.set(RedisConstants.AI_RAG_ACTIVE_CHUNKS_CACHE_KEY, chunks, ragProperties.getCacheTtl());
        } catch (RuntimeException ex) {
            // 缓存写入失败不影响检索主链路
        }
        return chunks;
    }

    private void invalidateCache() {
        redisOperator.delete(RedisConstants.AI_RAG_ACTIVE_CHUNKS_CACHE_KEY);
    }

    private List<Float> parseEmbedding(String embeddingJson) {
        try {
            List<Float> embedding = JsonUtils.fromJson(embeddingJson, new TypeReference<>() {
            });
            return embedding == null ? List.of() : embedding;
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private double cosineSimilarity(List<Float> left, List<Float> right) {
        int size = Math.min(left.size(), right.size());
        if (size == 0) {
            return 0D;
        }
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
