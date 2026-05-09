package com.cybzacg.blogbackend.module.forum.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 后台论坛帖子分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台论坛帖子分页查询参数")
public class ForumPostAdminPageQuery extends PageQuery {
    @Schema(description = "帖子标题关键字")
    private String keyword;

    @Schema(description = "版块ID")
    private Long sectionId;

    @Schema(description = "作者用户ID")
    private Long authorId;

    @Min(value = 0, message = "帖子状态不合法")
    @Max(value = 5, message = "帖子状态不合法")
    @Schema(description = "状态：0-草稿，1-已发布，2-审核中，3-已拒绝，4-已删除，5-隐藏")
    private Integer status;

    @Schema(description = "创建时间起始")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAtStart;

    @Schema(description = "创建时间截止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAtEnd;

    @Min(value = 0, message = "置顶状态不合法")
    @Max(value = 1, message = "置顶状态不合法")
    @Schema(description = "是否置顶：0-否，1-是")
    private Integer isTop;

    @Min(value = 0, message = "精华状态不合法")
    @Max(value = 1, message = "精华状态不合法")
    @Schema(description = "是否精华：0-否，1-是")
    private Integer isEssence;
}
