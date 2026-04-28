package com.cybzacg.blogbackend.module.article.model.admin;

import com.cybzacg.blogbackend.module.article.model.common.ArticleReviewLogVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 后台文章审核详情。
 */
@Data
@Schema(description = "后台文章审核详情")
public class ArticleReviewAdminDetailVO {
    @Schema(description = "文章详情")
    private ArticleDetailVO article;
    @Schema(description = "审核日志列表")
    private List<ArticleReviewLogVO> reviewLogs;
}
