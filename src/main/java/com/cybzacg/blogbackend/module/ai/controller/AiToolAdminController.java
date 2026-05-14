package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.*;
import com.cybzacg.blogbackend.module.ai.service.AiToolAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台 AI 工具管理控制器。
 */
@RestController
@RequestMapping("/api/sys/ai/tools")
@Tag(name = "后台AI工具管理")
@RequiredArgsConstructor
public class AiToolAdminController {

    private final AiToolAdminService aiToolAdminService;

    @GetMapping
    @Operation(summary = "分页查询工具定义")
    @PreAuthorize("@permission.hasPermission('ai:tool:query')")
    public Result<PageResult<AiToolDefinitionVO>> pageTools(AiToolDefinitionPageQuery query) {
        return Result.success(aiToolAdminService.pageTools(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询工具详情")
    @PreAuthorize("@permission.hasPermission('ai:tool:query')")
    public Result<AiToolDefinitionVO> getTool(@PathVariable Long id) {
        return Result.success(aiToolAdminService.getTool(id));
    }

    @PostMapping
    @Operation(summary = "创建工具定义")
    @PreAuthorize("@permission.hasPermission('ai:tool:create')")
    public Result<AiToolDefinitionVO> createTool(@Valid @RequestBody AiToolDefinitionSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiToolAdminService.createTool(request, operatorId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工具定义")
    @PreAuthorize("@permission.hasPermission('ai:tool:update')")
    public Result<AiToolDefinitionVO> updateTool(
            @PathVariable Long id,
            @Valid @RequestBody AiToolDefinitionSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiToolAdminService.updateTool(id, request, operatorId));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新工具状态")
    @PreAuthorize("@permission.hasPermission('ai:tool:update')")
    public Result<Void> updateToolStatus(@PathVariable Long id, @RequestParam Integer enabled) {
        Long operatorId = SecurityUtils.requireUserId();
        aiToolAdminService.updateToolStatus(id, enabled, operatorId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工具定义")
    @PreAuthorize("@permission.hasPermission('ai:tool:delete')")
    public Result<Void> deleteTool(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        aiToolAdminService.deleteTool(id, operatorId);
        return Result.success();
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "后台测试执行工具")
    @PreAuthorize("@permission.hasPermission('ai:tool:execute')")
    public Result<AiToolExecuteVO> executeTool(
            @PathVariable Long id,
            @Valid @RequestBody AiToolExecuteRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiToolAdminService.executeTool(id, request, operatorId));
    }

    @GetMapping("/call-logs")
    @Operation(summary = "分页查询工具调用日志")
    @PreAuthorize("@permission.hasPermission('ai:tool:query')")
    public Result<PageResult<AiToolCallLogVO>> pageCallLogs(AiToolCallLogPageQuery query) {
        return Result.success(aiToolAdminService.pageCallLogs(query));
    }

    @GetMapping("/authorizations")
    @Operation(summary = "分页查询工具授权")
    @PreAuthorize("@permission.hasPermission('ai:tool:query')")
    public Result<PageResult<AiToolAuthorizationVO>> pageAuthorizations(AiToolAuthorizationPageQuery query) {
        return Result.success(aiToolAdminService.pageAuthorizations(query));
    }

    @PostMapping("/authorizations")
    @Operation(summary = "创建工具授权")
    @PreAuthorize("@permission.hasPermission('ai:tool:update')")
    public Result<AiToolAuthorizationVO> createAuthorization(
            @Valid @RequestBody AiToolAuthorizationSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiToolAdminService.createAuthorization(request, operatorId));
    }

    @PutMapping("/authorizations/{id}")
    @Operation(summary = "更新工具授权")
    @PreAuthorize("@permission.hasPermission('ai:tool:update')")
    public Result<AiToolAuthorizationVO> updateAuthorization(
            @PathVariable Long id,
            @Valid @RequestBody AiToolAuthorizationSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiToolAdminService.updateAuthorization(id, request, operatorId));
    }

    @DeleteMapping("/authorizations/{id}")
    @Operation(summary = "删除工具授权")
    @PreAuthorize("@permission.hasPermission('ai:tool:update')")
    public Result<Void> deleteAuthorization(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        aiToolAdminService.deleteAuthorization(id, operatorId);
        return Result.success();
    }
}
