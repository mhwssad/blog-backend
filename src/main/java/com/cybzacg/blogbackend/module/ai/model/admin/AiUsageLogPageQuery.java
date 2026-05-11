package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI使用日志分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI使用日志分页查询条件")
public class AiUsageLogPageQuery extends PageQuery {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "成功状态：0-失败，1-成功")
    private Integer successStatus;

    @Schema(description = "每页条数")
    private Long size = 20L;
}
