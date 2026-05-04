package com.cybzacg.blogbackend.module.forum.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台论坛版块保存请求。
 */
@Data
@Schema(description = "后台论坛版块保存请求")
public class ForumSectionSaveRequest {
    @NotBlank(message = "版块名称不能为空")
    @Size(max = 64, message = "版块名称不能超过64个字符")
    @Schema(description = "版块名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 512, message = "版块简介不能超过512个字符")
    @Schema(description = "版块简介")
    private String description;

    @Schema(description = "排序值，越小越靠前")
    private Integer sortOrder;

    @Min(value = 0, message = "可见范围非法")
    @Max(value = 1, message = "可见范围非法")
    @Schema(description = "可见范围：0-公开，1-登录可见")
    private Integer visibilityScope;

    @Min(value = 1, message = "发帖最低等级不能小于1")
    @Schema(description = "发帖最低等级")
    private Integer postLevelLimit;

    @Min(value = 0, message = "状态非法")
    @Max(value = 1, message = "状态非法")
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
