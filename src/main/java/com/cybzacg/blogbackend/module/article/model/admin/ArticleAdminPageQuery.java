package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台文章分页查询条件")
public class ArticleAdminPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;
    @Schema(description = "每页条数")
    private Long size = 10L;
    @Schema(description = "搜索关键词")
    private String keyword;
    @Schema(description = "作者ID")
    private Long authorId;
    @Schema(description = "文章状态")
    private Integer status;
    @Schema(description = "访问级别")
    private Integer accessLevel;
    @Schema(description = "分类ID")
    private Long categoryId;
    @Schema(description = "标签ID")
    private Long tagId;
    @Schema(description = "是否置顶")
    private Integer isTop;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间开始")
    private LocalDateTime publishTimeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间结束")
    private LocalDateTime publishTimeEnd;
}
