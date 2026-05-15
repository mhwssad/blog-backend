package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.config.property.AiRagProperties;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.module.ai.service.AiEmbeddingService;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeChunkService;
import com.cybzacg.blogbackend.module.ai.service.AiVectorStore;
import com.cybzacg.blogbackend.utils.FileUtils;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 知识条目分块服务实现。
 *
 * <p>负责将知识条目的文本内容按字符数切分为固定大小的块（chunk），
 * 为每个块生成向量嵌入并存储到向量数据库，用于 RAG 检索。
 * 支持按条目重建（先删后插）和删除操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeChunkServiceImpl implements AiKnowledgeChunkService {
    private final AiEmbeddingService aiEmbeddingService;
    private final AiVectorStore aiVectorStore;
    private final AiRagProperties ragProperties;

    /**
     * 重建指定知识条目的所有分块：先将原始文本切分为片段，再为每个片段生成向量嵌入，
     * 最后批量写入向量数据库（先删后插）。
     *
     * @param entry 知识条目，必须包含非空的 id 和 contentSnapshot
     * @return 成功生成的分块数量
     */
    @Override
    public int rebuildChunks(AiKnowledgeEntry entry) {
        // 内容为空时清除已有分块并返回 0
        if (entry == null || entry.getId() == null || !StrUtils.hasText(entry.getContentSnapshot())) {
            if (entry != null && entry.getId() != null) {
                aiVectorStore.deleteByEntryId(entry.getId());
            }
            return 0;
        }
        List<String> texts = splitText(entry.getContentSnapshot());
        List<AiKnowledgeChunk> chunks = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            List<Float> embedding = aiEmbeddingService.embed(text);
            // 嵌入结果为空说明模型处理异常，跳过该片段
            if (embedding.isEmpty()) {
                continue;
            }
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setEntryId(entry.getId());
            chunk.setSourceType(entry.getSourceType());
            chunk.setSourceId(entry.getSourceId());
            chunk.setTitle(entry.getTitle());
            chunk.setSourceUrl(entry.getSourceUrl());
            chunk.setChunkIndex(i);
            chunk.setChunkText(text);
            chunk.setContentHash(sha256(text));
            chunk.setEmbeddingJson(JsonUtils.toJson(embedding));
            chunk.setEmbeddingDim(embedding.size());
            chunk.setEmbeddingModel(aiEmbeddingService.modelName());
            chunk.setStatus(1);
            chunk.setEntryVersion(entry.getVersion() != null ? entry.getVersion() : 1);
            chunks.add(chunk);
        }
        aiVectorStore.upsertChunks(entry.getId(), chunks);
        log.info("知识条目分块重建完成: entryId={}, chunkCount={}", entry.getId(), chunks.size());
        return chunks.size();
    }

    /**
     * 删除指定知识条目的所有分块及其向量索引。
     *
     * @param entryId 知识条目 ID
     */
    @Override
    public void deleteChunks(Long entryId) {
        aiVectorStore.deleteByEntryId(entryId);
    }

    /**
     * 按字符数切分文本，并保留轻量 overlap，适配中文内容。
     */
    List<String> splitText(String text) {
        if (!StrUtils.hasText(text)) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").trim();
        int chunkSize = Math.max(100, ragProperties.getChunkSize());
        int overlap = Math.max(0, Math.min(ragProperties.getChunkOverlap(), chunkSize / 2));
        if (normalized.length() <= chunkSize) {
            return List.of(normalized);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            chunks.add(normalized.substring(start, end).trim());
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks.stream().filter(StrUtils::hasText).toList();
    }

    /**
     * 计算文本的 SHA-256 哈希值，用于分块内容去重和变更检测。
     *
     * @param text 原始文本
     * @return 十六进制哈希字符串
     * @throws IllegalStateException 哈希算法不可用时抛出
     */
    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return FileUtils.toHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("计算知识分块哈希失败", ex);
        }
    }
}
