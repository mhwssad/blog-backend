package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 服务配置表。
 */
@Data
@TableName("ai_mcp_server_config")
public class AiMcpServerConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String serverName;
    private String transportType;
    private String connectionConfigJson;
    private String authConfigJson;
    private Integer timeoutSeconds;
    private Integer enabled;
    private String lastHealthStatus;
    private LocalDateTime lastDiscoveredAt;
    private String lastErrorSummary;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
