package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MCP 服务分页查询。
 */
@Data
@Schema(description = "MCP 服务分页查询")
public class AiMcpServerConfigPageQuery extends PageQuery {
    @Schema(description = "服务名称")
    private String serverName;

    @Schema(description = "传输类型")
    private String transportType;

    @Schema(description = "启用状态")
    private Integer enabled;
}
