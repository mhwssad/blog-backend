package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;

import java.util.List;

/**
 * AI 知识源抽取服务。
 *
 * <p>根据知识源类型（文章、帖子等）从业务数据库中抽取内容，转换为知识条目供分块和向量化入库。
 */
public interface AiKnowledgeSourceExtractor {

    /**
     * 抽取指定知识源下可入库的全部条目。
     *
     * @param sourceType 知识源类型编码（如 article、forum_post）
     * @return 抽取到的知识条目列表
     */
    List<AiKnowledgeEntry> extractAll(String sourceType);

    /**
     * 抽取指定来源对象。
     *
     * @param sourceType 知识源类型编码（如 article、forum_post）
     * @param sourceId   来源对象ID
     * @return 抽取到的单个知识条目
     */
    AiKnowledgeEntry extractOne(String sourceType, Long sourceId);
}
