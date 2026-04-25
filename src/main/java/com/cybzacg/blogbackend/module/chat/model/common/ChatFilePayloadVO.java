package com.cybzacg.blogbackend.module.chat.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

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

    @Schema(description = "预览地址，图片/语音可直接复用")
    private String previewUrl;

    @Schema(description = "缩略图地址，当前图片默认回落原图地址")
    private String thumbnailUrl;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;

    @Schema(description = "语音时长，单位秒")
    private Integer durationSeconds;

    @Schema(description = "语音波形采样点")
    private List<Integer> waveform;

    @Schema(description = "转码状态：source/pending/ready/failed")
    private String transcodeStatus;
}
