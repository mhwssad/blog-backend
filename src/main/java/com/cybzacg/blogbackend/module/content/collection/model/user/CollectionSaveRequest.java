package com.cybzacg.blogbackend.module.content.collection.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "用户收藏请求")
public class CollectionSaveRequest {
    @Schema(description = "收藏夹ID")
    private Long folderId;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID")
    private Long targetId;

    @NotBlank(message = "目标类型不能为空")
    @Schema(description = "目标类型")
    private String targetType;

    @Schema(description = "备注")
    private String remark;
}
