package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI 会话后台分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI会话后台分页查询条件")
public class AiSessionPageQuery extends PageQuery {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "会话状态：0-关闭，1-正常")
    private Integer status;

    @Schema(description = "渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "每页条数")
    private Long size = 20L;
}
