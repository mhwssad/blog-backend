package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.module.ai.service.AiEmbeddingService;
import com.cybzacg.blogbackend.utils.StrUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LangChain4j 本地 all-minilm embedding 服务实现。
 */
@Service
public class AiEmbeddingServiceImpl implements AiEmbeddingService {
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

    /**
     * 将文本转换为向量嵌入。
     *
     * @param text 待嵌入的文本内容，为空时返回空列表
     * @return 向量嵌入列表
     */
    @Override
    public List<Float> embed(String text) {
        if (!StrUtils.hasText(text)) {
            return List.of();
        }
        Response<Embedding> response = embeddingModel.embed(text);
        Embedding embedding = response.content();
        return embedding == null ? List.of() : embedding.vectorAsList();
    }

    /**
     * 获取当前使用的嵌入模型名称。
     *
     * @return 模型名称，默认 all-minilm-l6-v2-q
     */
    @Override
    public String modelName() {
        String modelName = embeddingModel.modelName();
        return StrUtils.hasText(modelName) ? modelName : "all-minilm-l6-v2-q";
    }
}
