package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.ai.AiMcpServerConfig;

/**
 * MCP 服务配置 Repository。
 */
public interface AiMcpServerConfigRepository extends IService<AiMcpServerConfig> {
    AiMcpServerConfig findByServerName(String serverName);
}
