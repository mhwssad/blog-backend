package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "文件业务引用详情")
public class FileReferenceVO {
    @Schema(description = "引用ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "引用类型")
    private String referenceType;
    @Schema(description = "引用对象ID")
    private Long referenceId;
    @Schema(description = "是否公开")
    private Integer isPublic;
    @Schema(description = "业务分类")
    private String category;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private Date createdAt;
}
