package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台上传任务分页查询条件")
public class FileTaskPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;
    @Schema(description = "每页条数")
    private Long size = 10L;
    @Schema(description = "上传用户ID")
    private Long uploadUserId;
    @Schema(description = "任务状态")
    private Integer taskStatus;
    @Schema(description = "是否秒传")
    private Integer isQuickUpload;
    @Schema(description = "是否分片")
    private Integer isChunked;
}
