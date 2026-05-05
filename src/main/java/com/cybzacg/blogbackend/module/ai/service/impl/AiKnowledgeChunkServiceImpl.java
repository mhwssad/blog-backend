package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.config.property.AiRagProperties;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.module.ai.service.AiEmbeddingService;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeChunkService;
import com.cybzacg.blogbackend.module.ai.service.AiVectorStore;
import com.cybzacg.blogbackend.utils.FileUtils;
import com.cybzacg.blogbackend.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 知识条目分块服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiKnowledgeChunkServiceImpl implements AiKnowledgeChunkService {
    private final AiEmbeddingService aiEmbeddingService;
    private final AiVectorStore aiVectorStore;
    private final AiRagProperties ragProperties;

    @Override
    public int rebuildChunks(AiKnowledgeEntry entry) {
        if (entry == null || entry.getId() == null || !StringUtils.hasText(entry.getContentSnapshot())) {
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
        return chunks.size();
    }

    @Override
    public void deleteChunks(Long entryId) {
        aiVectorStore.deleteByEntryId(entryId);
    }

    /**
     * 按字符数切分文本，并保留轻量 overlap，适配中文内容。
     */
    List<String> splitText(String text) {
        if (!StringUtils.hasText(text)) {
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
        return chunks.stream().filter(StringUtils::hasText).toList();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return FileUtils.toHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("计算知识分块哈希失败", ex);
        }
    }
}
