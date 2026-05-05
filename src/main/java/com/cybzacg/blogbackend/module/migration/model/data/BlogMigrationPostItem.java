package com.cybzacg.blogbackend.module.migration.model.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客迁移文章数据项。
 */
@Data
public class BlogMigrationPostItem {
    private String externalPostId;
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private List<String> categoryCodes;
    private List<String> tagNames;
    private Integer isOriginal;
    private String sourceUrl;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private List<BlogMigrationAttachmentItem> attachments;
}
