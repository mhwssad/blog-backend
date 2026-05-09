package com.cybzacg.blogbackend.module.article.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.enums.article.ArticleReviewStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章审核状态修正请求。
 */
@Data
@Schema(description = "文章审核状态修正请求")
public class ArticleReviewRepairRequest {
    @NotNull(message = "目标审核状态不能为空")
    @EnumValue(enumClass = ArticleReviewStatusEnum.class, message = "目标审核状态非法")
    @Schema(description = "目标审核状态：0 未送审，1 审核中，2 审核通过，3 审核拒绝")
    private Integer targetReviewStatus;

    @NotBlank(message = "修正说明不能为空")
    @Size(max = 512, message = "修正说明长度不能超过512个字符")
    @Schema(description = "修正说明")
    private String reviewComment;
}
