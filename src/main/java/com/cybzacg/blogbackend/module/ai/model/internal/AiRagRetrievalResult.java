package com.cybzacg.blogbackend.module.ai.model.internal;

import com.cybzacg.blogbackend.module.ai.model.common.AiRagReferenceVO;
import lombok.Data;

import java.util.List;

/**
 * RAG 检索结果。
 */
@Data
public class AiRagRetrievalResult {
    private boolean enabled;
    private long durationMs;
    private List<AiRagHit> hits = List.of();
    private List<AiRagReferenceVO> references = List.of();
    private String contextText;
    private String referenceJson;

    /**
     * 获取检索命中的分块数量。
     *
     * @return 命中数量
     */
    public int hitCount() {
        return hits == null ? 0 : hits.size();
    }
}
