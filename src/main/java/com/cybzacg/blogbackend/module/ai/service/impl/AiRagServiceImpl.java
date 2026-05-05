package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.config.property.AiRagProperties;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.enums.ai.AiDataScopeEnum;
import com.cybzacg.blogbackend.module.ai.model.common.AiRagReferenceVO;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagHit;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagRetrievalResult;
import com.cybzacg.blogbackend.module.ai.service.AiEmbeddingService;
import com.cybzacg.blogbackend.module.ai.service.AiRagService;
import com.cybzacg.blogbackend.module.ai.service.AiVectorStore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AI RAG 检索增强服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiRagServiceImpl implements AiRagService {
    private final AiRagProperties ragProperties;
    private final AiEmbeddingService aiEmbeddingService;
    private final AiVectorStore aiVectorStore;

    @Override
    public AiRagRetrievalResult retrieve(AiChannelConfig channelConfig, String question) {
        AiRagRetrievalResult result = new AiRagRetrievalResult();
        if (!Boolean.TRUE.equals(ragProperties.getEnabled()) || !StringUtils.hasText(question)
                || !allowsRag(channelConfig)) {
            return result;
        }
        long start = System.currentTimeMillis();
        result.setEnabled(true);
        List<Float> queryEmbedding = aiEmbeddingService.embed(question);
        List<AiRagHit> hits = aiVectorStore.search(
                queryEmbedding,
                ragProperties.getTopK(),
                ragProperties.getMinScore());
        result.setHits(hits);
        result.setReferences(hits.stream().map(this::toReference).toList());
        result.setContextText(buildContext(hits));
        result.setReferenceJson(JsonUtils.toJson(result.getReferences()));
        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public String enrichSystemPrompt(String systemPrompt, AiRagRetrievalResult retrievalResult) {
        if (retrievalResult == null || !retrievalResult.isEnabled()
                || !StringUtils.hasText(retrievalResult.getContextText())) {
            return systemPrompt;
        }
        String basePrompt = StringUtils.hasText(systemPrompt) ? systemPrompt : "";
        return basePrompt + """

                你可以参考以下知识库片段回答用户问题。
                要求：
                1. 优先基于引用内容回答。
                2. 如果引用内容不足以支撑答案，请明确说明“未在知识库中找到直接依据”。
                3. 不要编造不存在的来源或引用。

                知识库片段：
                """ + retrievalResult.getContextText();
    }

    private boolean allowsRag(AiChannelConfig channelConfig) {
        if (channelConfig == null || !StringUtils.hasText(channelConfig.getDataScopeJson())) {
            return true;
        }
        try {
            List<String> scopes = JsonUtils.fromJson(channelConfig.getDataScopeJson(), new TypeReference<>() {
            });
            if (scopes == null || scopes.isEmpty()) {
                return true;
            }
            Set<String> normalized = scopes.stream()
                    .filter(StringUtils::hasText)
                    .map(item -> item.trim().toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet());
            return normalized.contains(AiDataScopeEnum.PUBLIC_ARTICLES.getCode())
                    || normalized.contains(AiDataScopeEnum.PUBLIC_ARTICLES.name().toLowerCase(Locale.ROOT))
                    || normalized.contains(AiDataScopeEnum.PROFILE.getCode())
                    || normalized.contains(AiDataScopeEnum.PROFILE.name().toLowerCase(Locale.ROOT))
                    || normalized.contains(AiDataScopeEnum.PUBLIC_CHAT.getCode())
                    || normalized.contains(AiDataScopeEnum.PUBLIC_CHAT.name().toLowerCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String buildContext(List<AiRagHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (AiRagHit hit : hits) {
            AiKnowledgeChunk chunk = hit.getChunk();
            builder.append("[引用").append(index++).append("] ")
                    .append(chunk.getTitle()).append("，来源：")
                    .append(chunk.getSourceType()).append(":").append(chunk.getSourceId())
                    .append("，相似度：").append(String.format(Locale.ROOT, "%.4f", hit.getScore()))
                    .append("\n")
                    .append(chunk.getChunkText())
                    .append("\n\n");
        }
        return builder.toString();
    }

    private AiRagReferenceVO toReference(AiRagHit hit) {
        AiKnowledgeChunk chunk = hit.getChunk();
        AiRagReferenceVO vo = new AiRagReferenceVO();
        vo.setSourceType(chunk.getSourceType());
        vo.setSourceId(chunk.getSourceId());
        vo.setEntryId(chunk.getEntryId());
        vo.setTitle(chunk.getTitle());
        vo.setSourceUrl(chunk.getSourceUrl());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setScore(hit.getScore());
        return vo;
    }
}
