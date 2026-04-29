package com.cybzacg.blogbackend.module.content.collection.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台收藏分页查询条件")
public class CollectionPageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "收藏夹ID")
    private Long folderId;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
}
