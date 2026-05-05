package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ai.AiMcpToolSnapshot;

import java.util.List;

/**
 * MCP 工具快照 Repository。
 */
public interface AiMcpToolSnapshotRepository extends IService<AiMcpToolSnapshot> {
    List<AiMcpToolSnapshot> listByServerId(Long serverId);
}
