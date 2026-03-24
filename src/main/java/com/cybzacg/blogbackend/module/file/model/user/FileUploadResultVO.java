package com.cybzacg.blogbackend.module.file.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件上传结果")
public class FileUploadResultVO {
    @Schema(description = "上传标识")
    private String uploadId;
    @Schema(description = "任务ID")
    private Long taskId;
    @Schema(description = "文件ID")
    private Long fileId;
    @Schema(description = "业务引用ID")
    private Long businessId;
    @Schema(description = "是否秒传完成")
    private Boolean quickUpload;
    @Schema(description = "任务状态")
    private Integer taskStatus;
    @Schema(description = "文件访问地址")
    private String fileUrl;
    @Schema(description = "当前引用数")
    private Integer referenceCount;
}
