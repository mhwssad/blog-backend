package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolAuthorizationPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolAuthorizationSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolAuthorizationVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolCallLogPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolCallLogVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolDefinitionVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteVO;

/**
 * 后台 AI 工具管理服务。
 */
public interface AiToolAdminService {
    PageResult<AiToolDefinitionVO> pageTools(AiToolDefinitionPageQuery query);

    AiToolDefinitionVO getTool(Long id);

    AiToolDefinitionVO createTool(AiToolDefinitionSaveRequest request, Long operatorId);

    AiToolDefinitionVO updateTool(Long id, AiToolDefinitionSaveRequest request, Long operatorId);

    void updateToolStatus(Long id, Integer enabled, Long operatorId);

    void deleteTool(Long id, Long operatorId);

    AiToolExecuteVO executeTool(Long id, AiToolExecuteRequest request, Long operatorId);

    PageResult<AiToolCallLogVO> pageCallLogs(AiToolCallLogPageQuery query);

    PageResult<AiToolAuthorizationVO> pageAuthorizations(AiToolAuthorizationPageQuery query);

    AiToolAuthorizationVO createAuthorization(AiToolAuthorizationSaveRequest request, Long operatorId);

    AiToolAuthorizationVO updateAuthorization(Long id, AiToolAuthorizationSaveRequest request, Long operatorId);

    void deleteAuthorization(Long id, Long operatorId);
}
