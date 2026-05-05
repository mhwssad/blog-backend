package com.cybzacg.blogbackend.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 工具快照表。
 */
@Data
@TableName("ai_mcp_tool_snapshot")
public class AiMcpToolSnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long mcpServerId;
    private String mcpToolName;
    private String toolCode;
    private String toolName;
    private String description;
    private String parametersSchema;
    private String resultSchema;
    private String riskLevel;
    private String useScenarios;
    private Integer enabled;
    private LocalDateTime discoveredAt;
    private String rawDefinitionJson;
    private String lastErrorSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
