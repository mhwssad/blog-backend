package com.cybzacg.blogbackend.module.forum.model.publics;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公开论坛帖子分页查询条件")
public class ForumPostPageQuery extends PageQuery {
    @Schema(description = "搜索关键字")
    private String keyword;

    @Schema(description = "版块ID")
    private Long sectionId;

    @Schema(description = "作者ID")
    private Long authorId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间开始")
    private LocalDateTime createdAtStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间结束")
    private LocalDateTime createdAtEnd;

    @Schema(description = "排序方式：latest/hot", example = "latest")
    private String sort = "latest";
}
