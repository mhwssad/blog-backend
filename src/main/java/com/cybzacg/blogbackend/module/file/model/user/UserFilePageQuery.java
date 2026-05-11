package com.cybzacg.blogbackend.module.file.model.user;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.core.web.PageQuery;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户文件分页查询条件")
public class UserFilePageQuery extends PageQuery {
    @Schema(description = "文件名称关键字")
    private String keyword;

    @EnumValue(enumClass = FileStatusEnum.class, message = "文件状态值不合法")
    @Schema(description = "文件状态")
    private Integer status;

    @EnumValue(enumClass = FileCategoryEnum.class, message = "文件分类值不合法")
    @Schema(description = "业务分类")
    private String category;

    @EnumValue(enumClass = FileReferenceTypeEnum.class, message = "引用类型值不合法")
    @Schema(description = "引用类型")
    private String referenceType;
}
