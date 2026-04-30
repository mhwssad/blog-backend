package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户文件列表项（后台视角）。
 */
@Data
@Schema(description = "用户文件列表项")
public class UserFileVO {
    @Schema(description = "业务引用ID")
    private Long businessId;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "业务分类")
    private String category;

    @Schema(description = "引用类型")
    private String referenceType;

    @Schema(description = "引用对象ID")
    private Long referenceId;

    @Schema(description = "是否公开")
    private Integer isPublic;

    @Schema(description = "文件状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}