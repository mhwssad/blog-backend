package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.*;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeEntryAdminService;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSyncTaskAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台 AI 知识条目管理控制器。
 *
 * <p>负责知识条目的查询、状态管理和同步任务触发。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/ai/knowledge/entries")
@Tag(name = "后台AI知识条目")
@RequiredArgsConstructor
public class AiKnowledgeEntryAdminController {

    private final AiKnowledgeEntryAdminService aiKnowledgeEntryAdminService;
    private final AiKnowledgeSyncTaskAdminService aiKnowledgeSyncTaskAdminService;

    /**
     * 分页查询知识条目列表。
     *
     * @param query 分页查询参数，包含页码、页大小及可选筛选条件
     * @return 分页包装的知识条目视图对象
     */
    @GetMapping
    @Operation(summary = "分页查询知识条目")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<PageResult<AiKnowledgeEntryVO>> listEntries(AiKnowledgeEntryPageQuery query) {
        log.debug("后台分页查询知识条目: query={}", query);
        return Result.success(aiKnowledgeEntryAdminService.listEntries(query));
    }

    /**
     * 查询指定知识条目的详情。
     *
     * @param id 知识条目主键ID
     * @return 知识条目视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询知识条目详情")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<AiKnowledgeEntryVO> getEntry(@PathVariable Long id) {
        log.debug("后台查询知识条目详情: entryId={}", id);
        return Result.success(aiKnowledgeEntryAdminService.getEntry(id));
    }

    /**
     * 更新知识条目的状态（如启用、禁用等）。
     *
     * @param id     知识条目主键ID
     * @param status 目标状态值
     * @return 空结果
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新知识条目状态")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:update')")
    public Result<Void> updateEntryStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台更新知识条目状态: entryId={}, status={}, operatorId={}", id, status, operatorId);
        aiKnowledgeEntryAdminService.updateEntryStatus(id, status, operatorId);
        return Result.success();
    }

    /**
     * 触发知识同步任务，将知识条目同步到向量存储。
     *
     * @param request 同步触发请求体，包含同步范围等参数
     * @return 同步任务视图对象
     */
    @PostMapping("/sync")
    @Operation(summary = "触发知识同步任务")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:sync')")
    public Result<AiKnowledgeSyncTaskVO> triggerSync(
            @Valid @RequestBody AiKnowledgeSyncTriggerRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台触发知识同步任务: operatorId={}", operatorId);
        return Result.success(aiKnowledgeSyncTaskAdminService.triggerSync(request, operatorId));
    }

    /**
     * 分页查询知识同步任务列表。
     *
     * @param query 分页查询参数，包含页码、页大小及可选筛选条件
     * @return 分页包装的同步任务视图对象
     */
    @GetMapping("/sync/tasks")
    @Operation(summary = "分页查询同步任务")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<PageResult<AiKnowledgeSyncTaskVO>> listTasks(AiKnowledgeSyncTaskPageQuery query) {
        log.debug("后台分页查询知识同步任务: query={}", query);
        return Result.success(aiKnowledgeSyncTaskAdminService.listTasks(query));
    }

    /**
     * 查询指定知识同步任务的详情。
     *
     * @param taskId 同步任务主键ID
     * @return 同步任务视图对象
     */
    @GetMapping("/sync/tasks/{taskId}")
    @Operation(summary = "查询同步任务详情")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<AiKnowledgeSyncTaskVO> getTask(@PathVariable Long taskId) {
        log.debug("后台查询知识同步任务详情: taskId={}", taskId);
        return Result.success(aiKnowledgeSyncTaskAdminService.getTask(taskId));
    }

    /**
     * 重试失败的同步任务。
     *
     * @param taskId 同步任务主键ID
     * @return 空结果
     */
    @PostMapping("/sync/tasks/{taskId}/retry")
    @Operation(summary = "重试失败的同步任务")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:sync')")
    public Result<Void> retryTask(@PathVariable Long taskId) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台重试知识同步任务: taskId={}, operatorId={}", taskId, operatorId);
        aiKnowledgeSyncTaskAdminService.retryTask(taskId, operatorId);
        return Result.success();
    }
}
