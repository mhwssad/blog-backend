package com.cybzacg.blogbackend.module.file.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "初始化文件上传任务请求")
public class FileUploadInitRequest {

    @NotBlank(message = "原始文件名不能为空")
    @Schema(
        description = "原始文件名",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String originalName;

    @NotNull(message = "文件大小不能为空")
    @Schema(
        description = "文件大小（字节）",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long fileSize;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(
        description = "引用类型：avatar/chat_message/article_attachment/temp"
    )
    private String referenceType;

    @Schema(description = "引用对象ID")
    private Long referenceId;

    @Schema(description = "业务分类，如 avatar/attachment/comment/temp")
    private String category;

    @Schema(description = "是否公开：0-私有，1-公开")
    private Integer isPublic;

    @Schema(description = "总分片数，普通上传可为空")
    private Integer totalChunks;

    @Schema(description = "分片大小，普通上传可为空")
    private Long chunkSize;

    @Schema(description = "备注")
    private String remark;
}
