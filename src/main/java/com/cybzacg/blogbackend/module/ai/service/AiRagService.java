package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagRetrievalResult;

/**
 * AI RAG 检索增强服务。
 */
public interface AiRagService {

    /**
     * 根据用户问题检索知识库并生成模型上下文。
     */
    AiRagRetrievalResult retrieve(AiChannelConfig channelConfig, String question);

    /**
     * 将 RAG 上下文拼接到系统提示词。
     */
    String enrichSystemPrompt(String systemPrompt, AiRagRetrievalResult retrievalResult);
}
