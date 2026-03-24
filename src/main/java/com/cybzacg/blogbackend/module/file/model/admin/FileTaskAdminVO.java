package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "后台上传任务列表项")
public class FileTaskAdminVO {
    @Schema(description = "任务ID")
    private Long id;
    @Schema(description = "上传标识")
    private String uploadId;
    @Schema(description = "文件ID")
    private Long fileId;
    @Schema(description = "上传用户ID")
    private Long uploadUserId;
    @Schema(description = "原始文件名")
    private String originalName;
    @Schema(description = "文件大小")
    private Long fileSize;
    @Schema(description = "存储节点")
    private String storageKey;
    @Schema(description = "是否秒传")
    private Integer isQuickUpload;
    @Schema(description = "是否分片")
    private Integer isChunked;
    @Schema(description = "已上传分片数")
    private Integer uploadedChunks;
    @Schema(description = "总分片数")
    private Integer totalChunks;
    @Schema(description = "任务状态")
    private Integer taskStatus;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "创建时间")
    private Date createdAt;
    @Schema(description = "完成时间")
    private Date completeTime;
}
