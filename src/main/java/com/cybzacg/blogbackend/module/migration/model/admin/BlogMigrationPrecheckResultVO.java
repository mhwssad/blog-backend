package com.cybzacg.blogbackend.module.migration.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 博客迁移预检结果。
 */
@Data
@Schema(description = "博客迁移预检结果")
public class BlogMigrationPrecheckResultVO {
    private Long taskId;
    private Integer totalCount;
    private boolean passed;
    private List<BlogMigrationRecordVO> errors = new ArrayList<>();
}
