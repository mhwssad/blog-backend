package com.cybzacg.blogbackend.module.file.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户文件分页查询条件（后台视角
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户文件分页查询条件")
public class UserFilePageQuery extends PageQuery {

    @Schema(description = "文件名称关键字")
    private String keyword;

    @Schema(description = "文件状态")
    private Integer status;

    @Schema(description = "业务分类")
    private String category;

    @Schema(description = "引用类型")
    private String referenceType;
}
