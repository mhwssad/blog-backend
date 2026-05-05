package com.cybzacg.blogbackend.module.migration.model.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 外部附件下载入库结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogMigrationDownloadedAttachment {
    private Long fileId;
    private String fileUrl;
}
