package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通知分页查询条件")
public class SysNoticePageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "发布状态")
    private Integer publishStatus;

    @Schema(description = "目标类型")
    private Integer targetType;
}
