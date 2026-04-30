package com.cybzacg.blogbackend.module.file.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户上传初始化请求。
 */
@Data
@Schema(description = "用户文件上传初始化请求")
public class UserUploadInitRequest {
    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "引用类型")
    private String referenceType;

    @Schema(description = "引用对象ID")
    private Long referenceId;

    @Schema(description = "业务分类")
    private String category;

    @Schema(description = "是否公开：0-私有，1-公开")
    private Integer isPublic;

    @Schema(description = "总分片数")
    private Integer totalChunks;

    @Schema(description = "分片大小")
    private Long chunkSize;

    @Schema(description = "备注")
    private String remark;
}