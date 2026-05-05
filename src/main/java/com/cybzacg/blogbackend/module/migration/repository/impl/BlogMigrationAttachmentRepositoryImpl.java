package com.cybzacg.blogbackend.module.migration.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.migration.BlogMigrationAttachment;
import com.cybzacg.blogbackend.mapper.migration.BlogMigrationAttachmentMapper;
import com.cybzacg.blogbackend.module.migration.repository.BlogMigrationAttachmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 博客迁移附件 Repository 实现。
 */
@Repository
public class BlogMigrationAttachmentRepositoryImpl
        extends ServiceImpl<BlogMigrationAttachmentMapper, BlogMigrationAttachment>
        implements BlogMigrationAttachmentRepository {
    @Override
    public List<BlogMigrationAttachment> listByRecordId(Long recordId) {
        return list(new LambdaQueryWrapper<BlogMigrationAttachment>()
                .eq(BlogMigrationAttachment::getRecordId, recordId)
                .orderByAsc(BlogMigrationAttachment::getId));
    }

    @Override
    public boolean removeByTaskId(Long taskId) {
        return remove(new LambdaUpdateWrapper<BlogMigrationAttachment>()
                .eq(BlogMigrationAttachment::getTaskId, taskId));
    }
}
