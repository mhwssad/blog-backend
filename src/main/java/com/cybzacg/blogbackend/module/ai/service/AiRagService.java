package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagRetrievalResult;

/**
 * AI RAG 检索增强服务。
 *
 * <p>根据用户问题从向量知识库中检索相关内容，拼接到系统提示词中为模型提供上下文增强。
 */
public interface AiRagService {

    /**
     * 根据用户问题检索知识库并生成模型上下文。
     *
     * @param channelConfig 渠道配置（含 RAG 开关、检索参数等）
     * @param question      用户提问文本
     * @return RAG 检索结果（含命中文本、命中数量、耗时等）
     */
    AiRagRetrievalResult retrieve(AiChannelConfig channelConfig, String question);

    /**
     * 将 RAG 上下文拼接到系统提示词。
     *
     * @param systemPrompt     原始系统提示词
     * @param retrievalResult  RAG 检索结果
     * @return 拼接后的完整系统提示词
     */
    String enrichSystemPrompt(String systemPrompt, AiRagRetrievalResult retrievalResult);
}
