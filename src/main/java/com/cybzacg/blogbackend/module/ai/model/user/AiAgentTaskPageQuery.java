package com.cybzacg.blogbackend.module.ai.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 任务用户侧分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Agent 任务用户侧分页查询")
public class AiAgentTaskPageQuery extends PageQuery {
    @Schema(description = "状态：0-待执行 1-执行中 2-已完成 3-失败 4-已取消")
    private Integer status;
}
