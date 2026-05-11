package com.cybzacg.blogbackend.module.migration.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.migration.model.admin.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 外部博客迁移后台服务。
 */
public interface BlogMigrationAdminService {
    BlogMigrationTaskVO createTask(BlogMigrationCreateRequest request, MultipartFile file, Long operatorId);

    BlogMigrationPrecheckResultVO precheck(Long taskId, Long operatorId);

    BlogMigrationTaskVO execute(Long taskId, Long operatorId);

    PageResult<BlogMigrationTaskVO> pageTasks(BlogMigrationTaskPageQuery query);

    BlogMigrationTaskVO getTask(Long taskId);

    PageResult<BlogMigrationRecordVO> pageRecords(BlogMigrationRecordPageQuery query);

    byte[] exportFailures(Long taskId);
}
