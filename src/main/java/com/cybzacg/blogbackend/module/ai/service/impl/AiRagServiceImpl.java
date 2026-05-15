package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.config.property.AiRagProperties;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.enums.ai.AiDataScopeEnum;
import com.cybzacg.blogbackend.module.ai.model.common.AiRagReferenceVO;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagHit;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagRetrievalResult;
import com.cybzacg.blogbackend.module.ai.service.AiEmbeddingService;
import com.cybzacg.blogbackend.module.ai.service.AiRagService;
import com.cybzacg.blogbackend.module.ai.service.AiVectorStore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AI RAG 检索增强服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRagServiceImpl implements AiRagService {
    private final AiRagProperties ragProperties;
    private final AiEmbeddingService aiEmbeddingService;
    private final AiVectorStore aiVectorStore;

    /**
     * 对用户问题进行 RAG 检索：生成查询向量后在向量库中搜索相关分块。
     * 当 RAG 功能未启用、问题为空或渠道不允许 RAG 时返回空结果。
     *
     * @param channelConfig 渠道配置，用于判断是否允许 RAG
     * @param question      用户问题文本
     * @return 检索结果，包含命中分块、引用信息和耗时
     */
    @Override
    public AiRagRetrievalResult retrieve(AiChannelConfig channelConfig, String question) {
        AiRagRetrievalResult result = new AiRagRetrievalResult();
        if (!Boolean.TRUE.equals(ragProperties.getEnabled()) || !StrUtils.hasText(question)
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
        log.debug("RAG 检索完成: hitCount={}, durationMs={}", hits.size(), result.getDurationMs());
        return result;
    }

    /**
     * 将 RAG 检索结果注入系统提示词模板，附加知识库片段和引用规范要求。
     * 当检索结果为空或未启用时直接返回原始提示词。
     *
     * @param systemPrompt    原始系统提示词
     * @param retrievalResult RAG 检索结果
     * @return 注入知识库片段后的完整系统提示词
     */
    @Override
    public String enrichSystemPrompt(String systemPrompt, AiRagRetrievalResult retrievalResult) {
        if (retrievalResult == null || !retrievalResult.isEnabled()
                || !StrUtils.hasText(retrievalResult.getContextText())) {
            return systemPrompt;
        }
        String basePrompt = StrUtils.hasText(systemPrompt) ? systemPrompt : "";
        return basePrompt + """

                你可以参考以下知识库片段回答用户问题。
                要求：
                1. 优先基于引用内容回答。
                2. 如果引用内容不足以支撑答案，请明确说明“未在知识库中找到直接依据”。
                3. 不要编造不存在的来源或引用。

                知识库片段：
                """ + retrievalResult.getContextText();
    }

    /**
     * 判断渠道配置是否允许 RAG：解析 dataScopeJson，检查是否包含公共文章、个人资料或公共聊天等数据范围。
     * 配置为空或解析失败时默认允许，仅当显式配置且不包含上述范围时才拒绝。
     *
     * @param channelConfig 渠道配置
     * @return 是否允许 RAG
     */
    private boolean allowsRag(AiChannelConfig channelConfig) {
        if (channelConfig == null || !StrUtils.hasText(channelConfig.getDataScopeJson())) {
            return true;
        }
        try {
            List<String> scopes = JsonUtils.fromJson(channelConfig.getDataScopeJson(), new TypeReference<>() {
            });
            if (scopes == null || scopes.isEmpty()) {
                return true;
            }
            Set<String> normalized = scopes.stream()
                    .filter(StrUtils::hasText)
                    .map(StrUtils::trimToLowerCase)
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

    /**
     * 将命中分块拼接为知识库片段上下文文本，格式为 [引用N] 标题，来源：类型:ID，相似度：0.xxxx。
     *
     * @param hits 命中的分块列表
     * @return 拼接后的上下文文本
     */
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

    /**
     * 将 RAG 命中结果转换为引用 VO，提取来源、标题、分块索引和相似度分数。
     *
     * @param hit 命中的分块及相似度
     * @return 引用 VO
     */
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
