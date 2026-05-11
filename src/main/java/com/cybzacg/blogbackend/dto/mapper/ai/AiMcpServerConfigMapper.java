package com.cybzacg.blogbackend.dto.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.ai.AiMcpServerConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * MCP 服务配置 Mapper。
 */
@Mapper
public interface AiMcpServerConfigMapper
    extends BaseMapper<AiMcpServerConfig> {}
