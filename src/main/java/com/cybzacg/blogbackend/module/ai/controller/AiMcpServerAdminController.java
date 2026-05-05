package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpDiscoverResultVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpHealthVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpServerConfigPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpServerConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpServerConfigVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpToolSnapshotVO;
import com.cybzacg.blogbackend.module.ai.service.AiMcpServerAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台 MCP 服务管理控制器。
 */
@RestController
@RequestMapping("/api/sys/ai/mcp-servers")
@Tag(name = "后台AI MCP服务管理")
@RequiredArgsConstructor
public class AiMcpServerAdminController {

    private final AiMcpServerAdminService aiMcpServerAdminService;

    @GetMapping
    @Operation(summary = "分页查询 MCP 服务")
    @PreAuthorize("@permission.hasPermission('ai:mcp:query')")
    public Result<PageResult<AiMcpServerConfigVO>> pageServers(AiMcpServerConfigPageQuery query) {
        return Result.success(aiMcpServerAdminService.pageServers(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询 MCP 服务详情")
    @PreAuthorize("@permission.hasPermission('ai:mcp:query')")
    public Result<AiMcpServerConfigVO> getServer(@PathVariable Long id) {
        return Result.success(aiMcpServerAdminService.getServer(id));
    }

    @PostMapping
    @Operation(summary = "创建 MCP 服务")
    @PreAuthorize("@permission.hasPermission('ai:mcp:create')")
    public Result<AiMcpServerConfigVO> createServer(@Valid @RequestBody AiMcpServerConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiMcpServerAdminService.createServer(request, operatorId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 MCP 服务")
    @PreAuthorize("@permission.hasPermission('ai:mcp:update')")
    public Result<AiMcpServerConfigVO> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody AiMcpServerConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiMcpServerAdminService.updateServer(id, request, operatorId));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新 MCP 服务状态")
    @PreAuthorize("@permission.hasPermission('ai:mcp:update')")
    public Result<Void> updateServerStatus(@PathVariable Long id, @RequestParam Integer enabled) {
        Long operatorId = SecurityUtils.requireUserId();
        aiMcpServerAdminService.updateServerStatus(id, enabled, operatorId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 MCP 服务")
    @PreAuthorize("@permission.hasPermission('ai:mcp:delete')")
    public Result<Void> deleteServer(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        aiMcpServerAdminService.deleteServer(id, operatorId);
        return Result.success();
    }

    @PostMapping("/{id}/discover")
    @Operation(summary = "发现 MCP 工具")
    @PreAuthorize("@permission.hasPermission('ai:mcp:discover')")
    public Result<AiMcpDiscoverResultVO> discoverTools(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiMcpServerAdminService.discoverTools(id, operatorId));
    }

    @GetMapping("/{id}/tools")
    @Operation(summary = "查询 MCP 工具快照")
    @PreAuthorize("@permission.hasPermission('ai:mcp:query')")
    public Result<List<AiMcpToolSnapshotVO>> listTools(@PathVariable Long id) {
        return Result.success(aiMcpServerAdminService.listTools(id));
    }

    @GetMapping("/{id}/health")
    @Operation(summary = "查询 MCP 连接状态")
    @PreAuthorize("@permission.hasPermission('ai:mcp:query')")
    public Result<AiMcpHealthVO> checkHealth(@PathVariable Long id) {
        return Result.success(aiMcpServerAdminService.checkHealth(id));
    }
}
