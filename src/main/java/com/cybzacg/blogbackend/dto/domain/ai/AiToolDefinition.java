package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 工具定义表。
 */
@Data
@TableName("ai_tool_definition")
public class AiToolDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String toolCode;
    private String toolName;
    private String sourceType;
    private Long mcpServerId;
    private String mcpToolName;
    private String description;
    private String parametersSchema;
    private String resultSchema;
    private String riskLevel;
    private String useScenarios;
    private Integer enabled;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
