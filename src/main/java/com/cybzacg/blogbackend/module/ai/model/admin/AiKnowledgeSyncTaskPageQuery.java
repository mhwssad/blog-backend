package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识同步任务分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识同步任务分页查询")
public class AiKnowledgeSyncTaskPageQuery extends PageQuery {
    @Schema(description = "知识源类型")
    private String sourceType;
    @Schema(description = "状态：0-待执行，1-执行中，2-已完成，3-失败")
    private Integer status;
}
