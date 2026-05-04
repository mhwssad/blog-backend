package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionVO;
import com.cybzacg.blogbackend.module.ai.service.AiAgentDefinitionAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台 AI Agent 定义管理控制器。
 */
@RestController
@RequestMapping("/api/sys/ai/agents/definitions")
@Tag(name = "后台AI Agent定义管理")
@RequiredArgsConstructor
public class AiAgentDefinitionAdminController {

    private final AiAgentDefinitionAdminService aiAgentDefinitionAdminService;

    @GetMapping
    @Operation(summary = "分页查询 Agent 定义")
    @PreAuthorize("@permission.hasPermission('ai:agent:query')")
    public Result<PageResult<AiAgentDefinitionVO>> pageDefinitions(AiAgentDefinitionPageQuery query) {
        return Result.success(aiAgentDefinitionAdminService.pageDefinitions(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询 Agent 定义详情")
    @PreAuthorize("@permission.hasPermission('ai:agent:query')")
    public Result<AiAgentDefinitionVO> getDefinition(@PathVariable Long id) {
        return Result.success(aiAgentDefinitionAdminService.getDefinition(id));
    }

    @PostMapping
    @Operation(summary = "创建 Agent 定义")
    @PreAuthorize("@permission.hasPermission('ai:agent:create')")
    public Result<AiAgentDefinitionVO> createDefinition(
            @Valid @RequestBody AiAgentDefinitionSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiAgentDefinitionAdminService.createDefinition(request, operatorId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Agent 定义")
    @PreAuthorize("@permission.hasPermission('ai:agent:update')")
    public Result<AiAgentDefinitionVO> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody AiAgentDefinitionSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiAgentDefinitionAdminService.updateDefinition(id, request, operatorId));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换 Agent 启停状态")
    @PreAuthorize("@permission.hasPermission('ai:agent:update')")
    public Result<Void> toggleEnabled(
            @PathVariable Long id,
            @RequestParam Integer enabled) {
        Long operatorId = SecurityUtils.requireUserId();
        aiAgentDefinitionAdminService.toggleEnabled(id, enabled, operatorId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 Agent 定义")
    @PreAuthorize("@permission.hasPermission('ai:agent:delete')")
    public Result<Void> deleteDefinition(@PathVariable Long id) {
        aiAgentDefinitionAdminService.deleteDefinition(id);
        return Result.success();
    }
}
