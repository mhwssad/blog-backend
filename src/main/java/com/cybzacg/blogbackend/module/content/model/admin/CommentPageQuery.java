package com.cybzacg.blogbackend.module.content.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台评论分页查询条件")
public class CommentPageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
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
