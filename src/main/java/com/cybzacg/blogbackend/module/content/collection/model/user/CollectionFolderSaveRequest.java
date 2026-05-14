package com.cybzacg.blogbackend.module.content.collection.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户收藏夹保存请求")
public class CollectionFolderSaveRequest {
    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 64, message = "收藏夹名称最长64字符")
    @Schema(description = "收藏夹名称")
    private String folderName;

    @Schema(description = "收藏夹类型")
    private String folderType;

    @Size(max = 256, message = "描述最长256字符")
    @Schema(description = "描述")
    private String description;

    @Min(value = 0, message = "公开标识必须为 0 或 1")
    @Max(value = 1, message = "公开标识必须为 0 或 1")
    @Schema(description = "是否公开")
    private Integer isPublic;

    @Min(value = 0, message = "默认标识必须为 0 或 1")
    @Max(value = 1, message = "默认标识必须为 0 或 1")
    @Schema(description = "是否默认")
    private Integer isDefault;

    @Min(value = 0, message = "排序值不能为负数")
    @Schema(description = "排序")
    private Integer sortOrder;
}
