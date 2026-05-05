package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 工具调用日志分页查询。
 */
@Data
@Schema(description = "AI 工具调用日志分页查询")
public class AiToolCallLogPageQuery extends PageQuery {
    @Schema(description = "工具 ID")
    private Long toolId;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "Agent ID")
    private Long agentId;

    @Schema(description = "任务 ID")
    private Long taskId;

    @Schema(description = "成功状态：0-失败/1-成功")
    private Integer successStatus;
}
