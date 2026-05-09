package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "文章访问控制请求")
public class ArticleAccessAssignRequest {
    @Valid
    @Schema(description = "访问授权列表")
    private List<ArticleAccessItem> accessList;
}
