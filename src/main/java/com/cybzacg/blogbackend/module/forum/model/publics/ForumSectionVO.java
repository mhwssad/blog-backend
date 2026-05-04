package com.cybzacg.blogbackend.module.forum.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "公开论坛版块")
public class ForumSectionVO {
    @Schema(description = "版块ID")
    private Long id;
    @Schema(description = "版块名称")
    private String name;
    @Schema(description = "版块简介")
    private String description;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "可见范围：0-公开，1-登录可见")
    private Integer visibilityScope;
    @Schema(description = "发帖最低等级")
    private Integer postLevelLimit;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
