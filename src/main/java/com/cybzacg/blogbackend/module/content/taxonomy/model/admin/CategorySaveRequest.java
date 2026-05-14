package com.cybzacg.blogbackend.module.content.taxonomy.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "分类新增/修改请求")
public class CategorySaveRequest {
    @NotNull(message = "父分类不能为空")
    @Schema(description = "父分类ID")
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称最长64字符")
    @Schema(description = "分类名称")
    private String name;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码最长64字符")
    @Schema(description = "分类编码")
    private String code;

    @NotBlank(message = "分类类型不能为空")
    @Pattern(regexp = "article", message = "当前仅支持文章分类")
    @Schema(description = "分类类型")
    private String type;

    @Min(value = 0, message = "排序值不能为负数")
    @Schema(description = "排序")
    private Integer sortOrder;

    @Size(max = 512, message = "图标地址最长512字符")
    @Schema(description = "图标")
    private String icon;

    @Size(max = 256, message = "描述最长256字符")
    @Schema(description = "描述")
    private String description;

    @Min(value = 0, message = "状态必须为 0 或 1")
    @Max(value = 1, message = "状态必须为 0 或 1")
    @Schema(description = "状态")
    private Integer status;
}
