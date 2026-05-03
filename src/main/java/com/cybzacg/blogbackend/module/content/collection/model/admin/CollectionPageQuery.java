package com.cybzacg.blogbackend.module.content.collection.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台收藏分页查询条件")
public class CollectionPageQuery extends PageQuery {
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "收藏夹ID")
    private Long folderId;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
}
