package com.cybzacg.blogbackend.module.auth.experience.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 经验流水记录视图。
 */
@Data
@Builder
@Schema(description = "经验流水记录")
public class ExperienceLogVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "经验来源类型")
    private String sourceType;

    @Schema(description = "来源业务ID")
    private String sourceBizId;

    @Schema(description = "经验值")
    private Integer xpValue;

    @Schema(description = "入账日期")
    private LocalDate logDate;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
