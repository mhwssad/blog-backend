package com.cybzacg.blogbackend.dto.repository.migration.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationRecord;
import com.cybzacg.blogbackend.dto.mapper.migration.BlogMigrationRecordMapper;
import com.cybzacg.blogbackend.dto.repository.migration.BlogMigrationRecordRepository;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationRecordPageQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 博客迁移文章记录 Repository 实现。
 */
@Repository
public class BlogMigrationRecordRepositoryImpl
        extends ServiceImpl<BlogMigrationRecordMapper, BlogMigrationRecord>
        implements BlogMigrationRecordRepository {
    @Override
    public Page<BlogMigrationRecord> pageByQuery(BlogMigrationRecordPageQuery query) {
        LambdaQueryWrapper<BlogMigrationRecord> wrapper = new LambdaQueryWrapper<BlogMigrationRecord>()
                .eq(query.getTaskId() != null, BlogMigrationRecord::getTaskId, query.getTaskId())
                .eq(query.getStatus() != null, BlogMigrationRecord::getStatus, query.getStatus())
                .orderByAsc(BlogMigrationRecord::getId);
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    @Override
    public BlogMigrationRecord findByIdempotentKey(String idempotentKey) {
        return getOne(new LambdaQueryWrapper<BlogMigrationRecord>()
                .eq(BlogMigrationRecord::getIdempotentKey, idempotentKey)
                .eq(BlogMigrationRecord::getStatus, com.cybzacg.blogbackend.enums.migration.BlogMigrationRecordStatusEnum.SUCCESS.getValue())
                .isNotNull(BlogMigrationRecord::getTargetArticleId)
                .last("limit 1"), false);
    }

    @Override
    public BlogMigrationRecord findByTaskIdAndIdempotentKey(Long taskId, String idempotentKey) {
        return getOne(new LambdaQueryWrapper<BlogMigrationRecord>()
                .eq(BlogMigrationRecord::getTaskId, taskId)
                .eq(BlogMigrationRecord::getIdempotentKey, idempotentKey)
                .last("limit 1"), false);
    }

    @Override
    public List<BlogMigrationRecord> listByTaskId(Long taskId) {
        return list(new LambdaQueryWrapper<BlogMigrationRecord>()
                .eq(BlogMigrationRecord::getTaskId, taskId)
                .orderByAsc(BlogMigrationRecord::getId));
    }

    @Override
    public List<BlogMigrationRecord> listFailuresByTaskId(Long taskId) {
        return list(new LambdaQueryWrapper<BlogMigrationRecord>()
                .eq(BlogMigrationRecord::getTaskId, taskId)
                .eq(BlogMigrationRecord::getStatus, com.cybzacg.blogbackend.enums.migration.BlogMigrationRecordStatusEnum.FAILED.getValue())
                .orderByAsc(BlogMigrationRecord::getId));
    }

    @Override
    public long countByTaskIdAndStatus(Long taskId, Integer status) {
        return count(new LambdaQueryWrapper<BlogMigrationRecord>()
                .eq(BlogMigrationRecord::getTaskId, taskId)
                .eq(BlogMigrationRecord::getStatus, status));
    }

    @Override
    public boolean removeByTaskId(Long taskId) {
        return remove(new LambdaUpdateWrapper<BlogMigrationRecord>()
                .eq(BlogMigrationRecord::getTaskId, taskId));
    }
}
