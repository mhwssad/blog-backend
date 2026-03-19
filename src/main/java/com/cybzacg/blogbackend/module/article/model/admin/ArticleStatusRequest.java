package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "文章状态更新请求")
public class ArticleStatusRequest {
    @NotNull(message = "状态不能为空")
    @Schema(description = "文章状态")
    private Integer status;
}
