package com.cybzacg.blogbackend.dto.repository.migration;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationRecord;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationRecordPageQuery;

import java.util.List;

/**
 * 博客迁移文章记录 Repository。
 */
public interface BlogMigrationRecordRepository extends IService<BlogMigrationRecord> {
    Page<BlogMigrationRecord> pageByQuery(BlogMigrationRecordPageQuery query);

    BlogMigrationRecord findByIdempotentKey(String idempotentKey);

    BlogMigrationRecord findByTaskIdAndIdempotentKey(Long taskId, String idempotentKey);

    List<BlogMigrationRecord> listByTaskId(Long taskId);

    List<BlogMigrationRecord> listFailuresByTaskId(Long taskId);

    long countByTaskIdAndStatus(Long taskId, Integer status);

    boolean removeByTaskId(Long taskId);
}
