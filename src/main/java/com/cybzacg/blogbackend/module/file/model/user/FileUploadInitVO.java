package com.cybzacg.blogbackend.module.file.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "初始化文件上传任务响应")
public class FileUploadInitVO {
    @Schema(description = "上传任务ID")
    private Long taskId;
    @Schema(description = "上传标识")
    private String uploadId;
    @Schema(description = "上传模式")
    private Integer uploadMode;
    @Schema(description = "是否可秒传")
    private Boolean quickUploadAvailable;
    @Schema(description = "是否已通过秒传完成")
    private Boolean completed;
    @Schema(description = "总分片数")
    private Integer totalChunks;
    @Schema(description = "分片大小")
    private Long chunkSize;
    @Schema(description = "任务状态")
    private Integer taskStatus;
    @Schema(description = "已命中的文件ID")
    private Long fileId;
    @Schema(description = "文件访问地址")
    private String fileUrl;
    @Schema(description = "业务引用ID")
    private Long businessId;
}
