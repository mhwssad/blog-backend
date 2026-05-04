package com.cybzacg.blogbackend.module.forum.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "论坛帖子保存请求")
public class ForumPostSaveRequest {
    @NotNull(message = "版块ID不能为空")
    @Schema(description = "版块ID")
    private Long sectionId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题不能超过128个字符")
    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "状态：0-草稿，1-已发布")
    private Integer status;

    @Schema(description = "可见范围：0-公开，1-登录可见")
    private Integer visibilityScope;
}
