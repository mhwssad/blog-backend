package com.cybzacg.blogbackend.module.file.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户上传任务列表项")
public class UserFileTaskVO {
    @Schema(description = "任务ID")
    private Long id;
    @Schema(description = "上传标识")
    private String uploadId;
    @Schema(description = "文件ID")
    private Long fileId;
    @Schema(description = "文件名称")
    private String originalName;
    @Schema(description = "文件大小")
    private Long fileSize;
    @Schema(description = "是否秒传")
    private Integer isQuickUpload;
    @Schema(description = "是否分片")
    private Integer isChunked;
    @Schema(description = "分片大小")
    private Long chunkSize;
    @Schema(description = "总分片数")
    private Integer totalChunks;
    @Schema(description = "已上传分片数")
    private Integer uploadedChunks;
    @Schema(description = "任务状态")
    private Integer taskStatus;
    @Schema(description = "错误码")
    private String errorCode;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "完成时间")
    private LocalDateTime completeTime;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
