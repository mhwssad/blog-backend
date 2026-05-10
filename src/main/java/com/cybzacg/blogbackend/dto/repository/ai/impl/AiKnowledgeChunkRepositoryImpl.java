package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.dto.mapper.ai.AiKnowledgeChunkMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiKnowledgeChunkRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AiKnowledgeChunk Repository 实现。
 */
@Repository
public class AiKnowledgeChunkRepositoryImpl
        extends ServiceImpl<AiKnowledgeChunkMapper, AiKnowledgeChunk>
        implements AiKnowledgeChunkRepository {

    @Override
    public List<AiKnowledgeChunk> listActiveByEntryId(Long entryId) {
        return list(new LambdaQueryWrapper<AiKnowledgeChunk>()
                .eq(AiKnowledgeChunk::getEntryId, entryId)
                .eq(AiKnowledgeChunk::getStatus, 1)
                .orderByAsc(AiKnowledgeChunk::getChunkIndex));
    }

    @Override
    public List<AiKnowledgeChunk> listActiveForSearch(int limit) {
        int actualLimit = limit <= 0 ? 5000 : limit;
        return list(new LambdaQueryWrapper<AiKnowledgeChunk>()
                .eq(AiKnowledgeChunk::getStatus, 1)
                .orderByDesc(AiKnowledgeChunk::getUpdatedAt)
                .last("limit " + actualLimit));
    }

    @Override
    public void disableByEntryId(Long entryId) {
        update(new LambdaUpdateWrapper<AiKnowledgeChunk>()
                .eq(AiKnowledgeChunk::getEntryId, entryId)
                .eq(AiKnowledgeChunk::getStatus, 1)
                .set(AiKnowledgeChunk::getStatus, 0));
    }

    @Override
    public long countActiveByEntryId(Long entryId) {
        return count(new LambdaQueryWrapper<AiKnowledgeChunk>()
                .eq(AiKnowledgeChunk::getEntryId, entryId)
                .eq(AiKnowledgeChunk::getStatus, 1));
    }
}
