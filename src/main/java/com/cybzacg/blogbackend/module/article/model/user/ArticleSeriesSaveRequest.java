package com.cybzacg.blogbackend.module.article.model.user;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户侧系列保存请求。
 */
@Data
@Schema(description = "系列新增/修改请求")
public class ArticleSeriesSaveRequest {
    @NotBlank(message = "系列标题不能为空")
    @Size(max = 128, message = "系列标题长度不能超过128")
    @Schema(description = "系列标题")
    private String title;

    @Size(max = 1024, message = "系列描述长度不能超过1024")
    @Schema(description = "系列描述")
    private String description;

    @Size(max = 512, message = "系列封面长度不能超过512")
    @Schema(description = "系列封面")
    private String coverImage;

    @Min(value = 0, message = "系列状态必须为 0 或 1")
    @Max(value = 1, message = "系列状态必须为 0 或 1")
    @Schema(description = "系列状态：0-停用，1-正常")
    private Integer status;

    @EnumValue(enumClass = ArticleVisibilityScopeEnum.class, message = "可见范围值无效")
    @Schema(description = "可见范围：0-公开，1-仅自己可见，3-登录可见")
    private Integer visibilityScope;

    @Min(value = 0, message = "排序值不能为负数")
    @Schema(description = "排序值")
    private Integer sortOrder;
}
