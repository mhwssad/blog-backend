package com.cybzacg.blogbackend.module.ai.service;

import java.util.List;

/**
 * AI embedding 向量生成服务。
 */
public interface AiEmbeddingService {

    /**
     * 将文本转换为向量。
     */
    List<Float> embed(String text);

    /**
     * 当前 embedding 模型名称。
     */
    String modelName();
}
