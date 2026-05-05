package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.domain.ai.AiKnowledgeEntry;

import java.util.List;

/**
 * AI 知识源抽取服务。
 */
public interface AiKnowledgeSourceExtractor {

    /**
     * 抽取指定知识源下可入库的全部条目。
     */
    List<AiKnowledgeEntry> extractAll(String sourceType);

    /**
     * 抽取指定来源对象。
     */
    AiKnowledgeEntry extractOne(String sourceType, Long sourceId);
}
