package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI消息附件信息")
public class AttachmentVO {
    @Schema(description = "文件ID")
    private Long fileId;
    @Schema(description = "文件类型")
    private String fileType;
    @Schema(description = "MIME类型")
    private String mimeType;
    @Schema(description = "文件访问URL")
    private String fileUrl;
}
