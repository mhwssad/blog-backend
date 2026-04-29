package com.cybzacg.blogbackend.module.content.footprint.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户足迹分页查询条件")
public class UserFootprintPageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
    @Schema(description = "目标类型")
    private String targetType;
}
