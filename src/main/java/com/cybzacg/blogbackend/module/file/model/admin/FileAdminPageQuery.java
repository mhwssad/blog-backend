package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台文件分页查询条件")
public class FileAdminPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;
    @Schema(description = "每页条数")
    private Long size = 10L;
    @Schema(description = "关键字")
    private String keyword;
    @Schema(description = "上传用户ID")
    private Long uploadUserId;
    @Schema(description = "文件状态")
    private Integer status;
    @Schema(description = "业务分类")
    private String category;
    @Schema(description = "引用类型")
    private String referenceType;
    @Schema(description = "是否公开")
    private Integer isPublic;
}
