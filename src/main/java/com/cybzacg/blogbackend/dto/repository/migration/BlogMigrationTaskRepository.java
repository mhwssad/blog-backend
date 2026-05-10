package com.cybzacg.blogbackend.module.migration.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationTask;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationTaskPageQuery;

/**
 * 博客迁移任务 Repository。
 */
public interface BlogMigrationTaskRepository extends IService<BlogMigrationTask> {
    Page<BlogMigrationTask> pageByQuery(BlogMigrationTaskPageQuery query);
}
