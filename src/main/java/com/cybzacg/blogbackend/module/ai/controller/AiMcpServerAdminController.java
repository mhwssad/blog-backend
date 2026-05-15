package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.*;
import com.cybzacg.blogbackend.module.ai.service.AiMcpServerAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台 MCP 服务管理控制器。
 *
 * <p>负责 MCP 服务的增删改查、状态管理、工具发现与健康检查等后台运营接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/ai/mcp-servers")
@Tag(name = "后台AI MCP服务管理")
@RequiredArgsConstructor
public class AiMcpServerAdminController {

    private final AiMcpServerAdminService aiMcpServerAdminService;

    /**
     * 分页查询 MCP 服务列表。
     *
     * @param query 分页查询参数，包含页码、页大小及可选筛选条件
     * @return 分页包装的 MCP 服务配置视图对象
     */
    @GetMapping
    @Operation(summary = "分页查询 MCP 服务")
    @PreAuthorize("@permission.hasPermission('ai:mcp:query')")
    public Result<PageResult<AiMcpServerConfigVO>> pageServers(AiMcpServerConfigPageQuery query) {
        log.debug("后台分页查询MCP服务: query={}", query);
        return Result.success(aiMcpServerAdminService.pageServers(query));
    }

    /**
     * 查询指定 MCP 服务的详情。
     *
     * @param id MCP 服务主键ID
     * @return MCP 服务配置视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询 MCP 服务详情")
    @PreAuthorize("@permission.hasPermission('ai:mcp:query')")
    public Result<AiMcpServerConfigVO> getServer(@PathVariable Long id) {
        log.debug("后台查询MCP服务详情: serverId={}", id);
        return Result.success(aiMcpServerAdminService.getServer(id));
    }

    /**
     * 创建新的 MCP 服务配置。
     *
     * @param request MCP 服务保存请求体
     * @return 创建后的 MCP 服务配置视图对象
     */
    @PostMapping
    @Operation(summary = "创建 MCP 服务")
    @PreAuthorize("@permission.hasPermission('ai:mcp:create')")
    public Result<AiMcpServerConfigVO> createServer(@Valid @RequestBody AiMcpServerConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台创建MCP服务: operatorId={}", operatorId);
        return Result.success(aiMcpServerAdminService.createServer(request, operatorId));
    }

    /**
     * 更新指定 MCP 服务配置。
     *
     * @param id      MCP 服务主键ID
     * @param request MCP 服务保存请求体
     * @return 更新后的 MCP 服务配置视图对象
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新 MCP 服务")
    @PreAuthorize("@permission.hasPermission('ai:mcp:update')")
    public Result<AiMcpServerConfigVO> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody AiMcpServerConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台更新MCP服务: serverId={}, operatorId={}", id, operatorId);
        return Result.success(aiMcpServerAdminService.updateServer(id, request, operatorId));
    }

    /**
     * 更新 MCP 服务的启用/禁用状态。
     *
     * @param id      MCP 服务主键ID
     * @param enabled 目标状态值（1-启用，0-禁用）
     * @return 空结果
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新 MCP 服务状态")
    @PreAuthorize("@permission.hasPermission('ai:mcp:update')")
    public Result<Void> updateServerStatus(@PathVariable Long id, @RequestParam Integer enabled) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台更新MCP服务状态: serverId={}, enabled={}, operatorId={}", id, enabled, operatorId);
        aiMcpServerAdminService.updateServerStatus(id, enabled, operatorId);
        return Result.success();
    }

    /**
     * 删除指定 MCP 服务。
     *
     * @param id MCP 服务主键ID
     * @return 空结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除 MCP 服务")
    @PreAuthorize("@permission.hasPermission('ai:mcp:delete')")
    public Result<Void> deleteServer(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台删除MCP服务: serverId={}, operatorId={}", id, operatorId);
        aiMcpServerAdminService.deleteServer(id, operatorId);
        return Result.success();
    }

    /**
     * 发现并注册指定 MCP 服务提供的工具列表。
     *
     * @param id MCP 服务主键ID
     * @return 工具发现结果视图对象
     */
    @PostMapping("/{id}/discover")
    @Operation(summary = "发现 MCP 工具")
    @PreAuthorize("@permission.hasPermission('ai:mcp:discover')")
    public Result<AiMcpDiscoverResultVO> discoverTools(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台发现MCP工具: serverId={}, operatorId={}", id, operatorId);
        return Result.success(aiMcpServerAdminService.discoverTools(id, operatorId));
    }

    /**
     * 查询指定 MCP 服务的工具快照列表。
     *
     * @param id MCP 服务主键ID
     * @return MCP 工具快照视图对象列表
     */
    @GetMapping("/{id}/tools")
    @Operation(summary = "查询 MCP 工具快照")
    @PreAuthorize("@permission.hasPermission('ai:mcp:query')")
    public Result<List<AiMcpToolSnapshotVO>> listTools(@PathVariable Long id) {
        log.debug("后台查询MCP工具快照: serverId={}", id);
        return Result.success(aiMcpServerAdminService.listTools(id));
    }

    /**
     * 检查指定 MCP 服务的连接健康状态。
     *
     * @param id MCP 服务主键ID
     * @return MCP 健康检查视图对象
     */
    @GetMapping("/{id}/health")
    @Operation(summary = "查询 MCP 连接状态")
    @PreAuthorize("@permission.hasPermission('ai:mcp:query')")
    public Result<AiMcpHealthVO> checkHealth(@PathVariable Long id) {
        log.debug("后台检查MCP服务健康状态: serverId={}", id);
        return Result.success(aiMcpServerAdminService.checkHealth(id));
    }
}
