package com.cybzacg.blogbackend.module.migration.model.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客迁移文章数据项。
 */
@Data
public class BlogMigrationPostItem {
    @NotBlank(message = "外部文章ID不能为空")
    private String externalPostId;
    @NotBlank(message = "文章标题不能为空")
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
