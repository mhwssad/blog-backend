package com.cybzacg.blogbackend.module.file.model.admin;

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
@Schema(description = "后台文件分页查询条件")
public class FileAdminPageQuery extends PageQuery {
    @Schema(description = "关键字")
    private String keyword;
    @Schema(description = "上传用户ID")
    private Long uploadUserId;
    @EnumValue(enumClass = FileStatusEnum.class, message = "文件状态值无效")
    @Schema(description = "文件状态")
    private Integer status;
    @EnumValue(enumClass = FileCategoryEnum.class, message = "文件分类值无效")
    @Schema(description = "业务分类")
    private String category;
    @EnumValue(enumClass = FileReferenceTypeEnum.class, message = "引用类型值无效")
    @Schema(description = "引用类型")
    private String referenceType;
    @Schema(description = "是否公开")
    private Integer isPublic;
}
