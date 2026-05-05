package com.cybzacg.blogbackend.module.ai.model.internal;

import com.cybzacg.blogbackend.domain.ai.AiKnowledgeChunk;
import lombok.Builder;
import lombok.Data;

/**
 * RAG 检索命中的分块。
 */
@Data
@Builder
public class AiRagHit {
    private AiKnowledgeChunk chunk;
    private double score;
}
