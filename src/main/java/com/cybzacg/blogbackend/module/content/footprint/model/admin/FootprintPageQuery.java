package com.cybzacg.blogbackend.module.content.footprint.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台足迹分页查询条件")
public class FootprintPageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "访问开始时间")
    private LocalDateTime visitedAtStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "访问结束时间")
    private LocalDateTime visitedAtEnd;
}
