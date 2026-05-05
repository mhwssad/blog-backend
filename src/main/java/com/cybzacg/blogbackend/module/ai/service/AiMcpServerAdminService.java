package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpDiscoverResultVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpHealthVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpServerConfigPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpServerConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpServerConfigVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpToolSnapshotVO;

import java.util.List;

/**
 * 后台 MCP 服务管理服务。
 */
public interface AiMcpServerAdminService {
    PageResult<AiMcpServerConfigVO> pageServers(AiMcpServerConfigPageQuery query);

    AiMcpServerConfigVO getServer(Long id);

    AiMcpServerConfigVO createServer(AiMcpServerConfigSaveRequest request, Long operatorId);

    AiMcpServerConfigVO updateServer(Long id, AiMcpServerConfigSaveRequest request, Long operatorId);

    void updateServerStatus(Long id, Integer enabled, Long operatorId);

    void deleteServer(Long id, Long operatorId);

    AiMcpDiscoverResultVO discoverTools(Long id, Long operatorId);

    List<AiMcpToolSnapshotVO> listTools(Long id);

    AiMcpHealthVO checkHealth(Long id);
}
