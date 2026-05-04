package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.mapper.ai.AiKnowledgeEntryMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiKnowledgeEntryRepository;
import org.springframework.stereotype.Repository;

/**
 * AiKnowledgeEntry Repository 实现。
 */
@Repository
public class AiKnowledgeEntryRepositoryImpl
        extends ServiceImpl<AiKnowledgeEntryMapper, AiKnowledgeEntry>
        implements AiKnowledgeEntryRepository {

    @Override
    public AiKnowledgeEntry findBySource(String sourceType, Long sourceId) {
        return getOne(new LambdaQueryWrapper<AiKnowledgeEntry>()
                .eq(AiKnowledgeEntry::getSourceType, sourceType)
                .eq(AiKnowledgeEntry::getSourceId, sourceId)
                .last("limit 1"), false);
    }

    @Override
    public Page<AiKnowledgeEntry> pageByQuery(String sourceType, Integer status,
                                               String keyword, long current, long size) {
        LambdaQueryWrapper<AiKnowledgeEntry> wrapper = new LambdaQueryWrapper<AiKnowledgeEntry>()
                .eq(sourceType != null, AiKnowledgeEntry::getSourceType, sourceType)
                .eq(status != null, AiKnowledgeEntry::getStatus, status)
                .like(keyword != null, AiKnowledgeEntry::getTitle, keyword)
                .ne(AiKnowledgeEntry::getStatus, 3)
                .orderByDesc(AiKnowledgeEntry::getUpdatedAt);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public long countBySourceTypeAndStatus(String sourceType, Integer status) {
        return count(new LambdaQueryWrapper<AiKnowledgeEntry>()
                .eq(sourceType != null, AiKnowledgeEntry::getSourceType, sourceType)
                .eq(status != null, AiKnowledgeEntry::getStatus, status));
    }
}
