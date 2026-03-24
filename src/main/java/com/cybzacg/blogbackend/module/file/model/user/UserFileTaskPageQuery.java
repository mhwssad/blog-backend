package com.cybzacg.blogbackend.module.file.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户上传任务分页查询条件")
public class UserFileTaskPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;
    @Schema(description = "每页条数")
    private Long size = 10L;
    @Schema(description = "任务状态")
    private Integer taskStatus;
    @Schema(description = "是否秒传：0-否，1-是")
    private Integer isQuickUpload;
    @Schema(description = "是否分片：0-否，1-是")
    private Integer isChunked;
}
