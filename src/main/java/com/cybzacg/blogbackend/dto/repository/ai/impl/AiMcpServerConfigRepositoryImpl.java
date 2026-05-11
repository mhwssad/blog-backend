package com.cybzacg.blogbackend.dto.repository.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiMcpServerConfig;
import com.cybzacg.blogbackend.dto.mapper.ai.AiMcpServerConfigMapper;
import com.cybzacg.blogbackend.dto.repository.ai.AiMcpServerConfigRepository;
import org.springframework.stereotype.Repository;

/**
 * MCP 服务配置 Repository 实现。
 */
@Repository
public class AiMcpServerConfigRepositoryImpl extends ServiceImpl<AiMcpServerConfigMapper, AiMcpServerConfig>
        implements AiMcpServerConfigRepository {

    @Override
    public AiMcpServerConfig findByServerName(String serverName) {
        return getOne(new LambdaQueryWrapper<AiMcpServerConfig>()
                .eq(AiMcpServerConfig::getServerName, serverName)
                .last("limit 1"), false);
    }
}
