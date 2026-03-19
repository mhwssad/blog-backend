package com.cybzacg.blogbackend.module.content.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "前台分类树节点")
public class PublicCategoryTreeVO {
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
    @Schema(description = "层级")
    private Integer level;
    @Schema(description = "排序")
    private Integer sortOrder;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "子分类列表")
    private List<PublicCategoryTreeVO> children = new ArrayList<>();
}
