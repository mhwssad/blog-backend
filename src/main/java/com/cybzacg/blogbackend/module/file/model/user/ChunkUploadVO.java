package com.cybzacg.blogbackend.module.file.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分片上传结果")
public class ChunkUploadVO {
    @Schema(description = "上传标识")
    private String uploadId;
    @Schema(description = "分片序号")
    private Integer chunkNumber;
    @Schema(description = "已上传分片数")
    private Integer uploadedChunks;
    @Schema(description = "总分片数")
    private Integer totalChunks;
    @Schema(description = "任务状态")
    private Integer taskStatus;
}
