package com.cybzacg.blogbackend.module.content.collection.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户收藏夹保存请求")
public class CollectionFolderSaveRequest {
    @NotBlank(message = "收藏夹名称不能为空")
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
}
