package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 工具定义分页查询。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "AI 工具定义分页查询")
public class AiToolDefinitionPageQuery extends PageQuery {
    @Schema(description = "工具编码")
    private String toolCode;

    @Schema(description = "工具名称")
    private String toolName;

    @Schema(description = "来源类型 builtin/mcp")
    private String sourceType;

    @Schema(description = "是否启用：0-否/1-是")
    private Integer enabled;
}
