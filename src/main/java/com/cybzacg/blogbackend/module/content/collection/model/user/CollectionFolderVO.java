package com.cybzacg.blogbackend.module.content.collection.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户收藏夹信息")
public class CollectionFolderVO {
    @Schema(description = "收藏夹ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "收藏夹名称")
    private String folderName;
    @Schema(description = "收藏夹类型")
    private String folderType;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "是否公开")
    private Integer isPublic;
    @Schema(description = "是否默认")
    private Integer isDefault;
    @Schema(description = "排序")
    private Integer sortOrder;
    @Schema(description = "收藏数量")
    private Integer collectionCount;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
