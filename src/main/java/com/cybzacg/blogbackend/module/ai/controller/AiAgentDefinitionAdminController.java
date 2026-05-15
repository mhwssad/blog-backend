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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台 AI Agent 定义管理控制器。
 *
 * <p>负责 Agent 定义的增删改查及启停状态管理等后台运营接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/ai/agents/definitions")
@Tag(name = "后台AI Agent定义管理")
@RequiredArgsConstructor
public class AiAgentDefinitionAdminController {

    private final AiAgentDefinitionAdminService aiAgentDefinitionAdminService;

    /**
     * 分页查询 Agent 定义列表。
     *
     * @param query 分页查询参数，包含页码、页大小及可选筛选条件
     * @return 分页包装的 Agent 定义视图对象
     */
    @GetMapping
    @Operation(summary = "分页查询 Agent 定义")
    @PreAuthorize("@permission.hasPermission('ai:agent:query')")
    public Result<PageResult<AiAgentDefinitionVO>> pageDefinitions(AiAgentDefinitionPageQuery query) {
        log.debug("后台分页查询Agent定义: query={}", query);
        return Result.success(aiAgentDefinitionAdminService.pageDefinitions(query));
    }

    /**
     * 查询指定 Agent 定义的详情。
     *
     * @param id Agent 定义主键ID
     * @return Agent 定义视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询 Agent 定义详情")
    @PreAuthorize("@permission.hasPermission('ai:agent:query')")
    public Result<AiAgentDefinitionVO> getDefinition(@PathVariable Long id) {
        log.debug("后台查询Agent定义详情: definitionId={}", id);
        return Result.success(aiAgentDefinitionAdminService.getDefinition(id));
    }

    /**
     * 创建新的 Agent 定义。
     *
     * @param request Agent 定义保存请求体
     * @return 创建后的 Agent 定义视图对象
     */
    @PostMapping
    @Operation(summary = "创建 Agent 定义")
    @PreAuthorize("@permission.hasPermission('ai:agent:create')")
    public Result<AiAgentDefinitionVO> createDefinition(
            @Valid @RequestBody AiAgentDefinitionSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台创建Agent定义: operatorId={}", operatorId);
        return Result.success(aiAgentDefinitionAdminService.createDefinition(request, operatorId));
    }

    /**
     * 更新指定 Agent 定义。
     *
     * @param id      Agent 定义主键ID
     * @param request Agent 定义保存请求体
     * @return 更新后的 Agent 定义视图对象
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新 Agent 定义")
    @PreAuthorize("@permission.hasPermission('ai:agent:update')")
    public Result<AiAgentDefinitionVO> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody AiAgentDefinitionSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台更新Agent定义: definitionId={}, operatorId={}", id, operatorId);
        return Result.success(aiAgentDefinitionAdminService.updateDefinition(id, request, operatorId));
    }

    /**
     * 切换 Agent 定义的启用/禁用状态。
     *
     * @param id      Agent 定义主键ID
     * @param enabled 目标状态值（1-启用，0-禁用）
     * @return 空结果
     */
    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换 Agent 启停状态")
    @PreAuthorize("@permission.hasPermission('ai:agent:update')")
    public Result<Void> toggleEnabled(
            @PathVariable Long id,
            @RequestParam Integer enabled) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台切换Agent启停状态: definitionId={}, enabled={}, operatorId={}", id, enabled, operatorId);
        aiAgentDefinitionAdminService.toggleEnabled(id, enabled, operatorId);
        return Result.success();
    }

    /**
     * 删除指定 Agent 定义。
     *
     * @param id Agent 定义主键ID
     * @return 空结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除 Agent 定义")
    @PreAuthorize("@permission.hasPermission('ai:agent:delete')")
    public Result<Void> deleteDefinition(@PathVariable Long id) {
        log.info("后台删除Agent定义: definitionId={}", id);
        aiAgentDefinitionAdminService.deleteDefinition(id);
        return Result.success();
    }
}
