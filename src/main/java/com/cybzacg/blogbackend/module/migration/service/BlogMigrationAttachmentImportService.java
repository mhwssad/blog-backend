package com.cybzacg.blogbackend.module.migration.service;

import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationTask;
import com.cybzacg.blogbackend.module.migration.model.data.BlogMigrationAttachmentItem;
import com.cybzacg.blogbackend.module.migration.model.internal.BlogMigrationDownloadedAttachment;

/**
 * 博客迁移附件下载入库服务。
 */
public interface BlogMigrationAttachmentImportService {
    BlogMigrationDownloadedAttachment downloadAndSave(BlogMigrationTask task, BlogMigrationAttachmentItem attachmentItem);
}
