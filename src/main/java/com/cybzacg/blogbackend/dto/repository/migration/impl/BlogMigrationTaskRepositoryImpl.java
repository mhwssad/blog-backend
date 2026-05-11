package com.cybzacg.blogbackend.dto.repository.migration.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationTask;
import com.cybzacg.blogbackend.dto.mapper.migration.BlogMigrationTaskMapper;
import com.cybzacg.blogbackend.dto.repository.migration.BlogMigrationTaskRepository;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationTaskPageQuery;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Repository;

/**
 * 博客迁移任务 Repository 实现。
 */
@Repository
public class BlogMigrationTaskRepositoryImpl
        extends ServiceImpl<BlogMigrationTaskMapper, BlogMigrationTask>
        implements BlogMigrationTaskRepository {
    @Override
    public Page<BlogMigrationTask> pageByQuery(BlogMigrationTaskPageQuery query) {
        LambdaQueryWrapper<BlogMigrationTask> wrapper = new LambdaQueryWrapper<BlogMigrationTask>()
                .eq(query.getStatus() != null, BlogMigrationTask::getStatus, query.getStatus())
                .eq(query.getAuthorId() != null, BlogMigrationTask::getAuthorId, query.getAuthorId())
                .eq(StrUtils.hasText(query.getSourcePlatform()), BlogMigrationTask::getSourcePlatform, query.getSourcePlatform())
                .orderByDesc(BlogMigrationTask::getId);
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }
}
