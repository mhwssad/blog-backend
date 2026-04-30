package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户上传任务 VO。
 */
@Data
@Schema(description = "用户上传任务视图对象")
public class UserTaskVO {
    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "上传标识")
    private String uploadId;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "业务ID")
    private Long businessId;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "分片大小")
    private Long chunkSize;

    @Schema(description = "总分片数")
    private Integer totalChunks;

    @Schema(description = "已上传分片数")
    private Integer uploadedChunks;

    @Schema(description = "分片编号（仅在上传分片时使用）")
    private Integer chunkNumber;

    @Schema(description = "上传模式：0-整文件上传，1-秒传，2-分片上传")
    private Integer uploadMode;

    @Schema(description = "是否秒传")
    private Boolean quickUpload;

    @Schema(description = "秒传是否可用")
    private Boolean quickUploadAvailable;

    @Schema(description = "是否完成")
    private Boolean completed;

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