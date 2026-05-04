package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 定义分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Agent 定义分页查询")
public class AiAgentDefinitionPageQuery extends PageQuery {
    @Schema(description = "名称关键词")
    private String keyword;
    @Schema(description = "启用状态：0-停用，1-启用")
    private Integer enabled;
}
