package com.cybzacg.blogbackend.module.ai.service;

import java.util.List;

/**
 * AI embedding 向量生成服务。
 *
 * <p>将文本转换为高维向量，供知识库语义检索和相似度计算使用。
 */
public interface AiEmbeddingService {

    /**
     * 将文本转换为向量。
     *
     * @param text 待转换的文本内容
     * @return 向量浮点数列表
     */
    List<Float> embed(String text);

    /**
     * 当前 embedding 模型名称。
     *
     * @return 模型名称字符串
     */
    String modelName();
}
