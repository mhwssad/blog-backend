package com.cybzacg.blogbackend.dto.repository.migration;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationAttachment;

import java.util.List;

/**
 * 博客迁移附件 Repository。
 */
public interface BlogMigrationAttachmentRepository extends IService<BlogMigrationAttachment> {
    List<BlogMigrationAttachment> listByRecordId(Long recordId);

    boolean removeByTaskId(Long taskId);
}
