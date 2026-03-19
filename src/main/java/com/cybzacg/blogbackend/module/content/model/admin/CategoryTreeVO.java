package com.cybzacg.blogbackend.module.content.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "后台分类树节点")
public class CategoryTreeVO {
    @Schema(description = "分类ID")
    private Long id;
    @Schema(description = "父分类ID")
    private Long parentId;
    @Schema(description = "分类名称")
    private String name;
    @Schema(description = "分类编码")
    private String code;
    @Schema(description = "分类类型")
    private String type;
    @Schema(description = "祖级路径")
    private String ancestors;
    @Schema(description = "层级")
    private Integer level;
    @Schema(description = "排序")
    private Integer sortOrder;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "子分类列表")
    private List<CategoryTreeVO> children = new ArrayList<>();
}
