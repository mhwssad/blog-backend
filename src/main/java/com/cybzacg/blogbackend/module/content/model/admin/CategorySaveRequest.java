package com.cybzacg.blogbackend.module.content.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "分类新增/修改请求")
public class CategorySaveRequest {
    @NotNull(message = "父分类不能为空")
    @Schema(description = "父分类ID")
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称")
    private String name;

    @NotBlank(message = "分类编码不能为空")
    @Schema(description = "分类编码")
    private String code;

    @NotBlank(message = "分类类型不能为空")
    @Schema(description = "分类类型")
    private String type;

    @Schema(description = "排序")
    private Integer sortOrder;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "状态")
    private Integer status;
}
