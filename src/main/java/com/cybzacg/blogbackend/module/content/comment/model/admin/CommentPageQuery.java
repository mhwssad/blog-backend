package com.cybzacg.blogbackend.module.content.comment.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台评论分页查询条件")
public class CommentPageQuery extends PageQuery {
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "根评论ID")
    private Long rootId;
    @Schema(description = "父评论ID")
    private Long parentId;
    @Schema(description = "评论状态")
    private Integer status;
}
