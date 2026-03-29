package com.cybzacg.blogbackend.module.chat.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件消息载荷。
 */
@Data
@Schema(description = "文件消息载荷")
public class ChatFilePayloadVO {
    @Schema(description = "聊天文件业务引用ID")
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

    @Schema(description = "MIME 类型")
    private String mimeType;
}
