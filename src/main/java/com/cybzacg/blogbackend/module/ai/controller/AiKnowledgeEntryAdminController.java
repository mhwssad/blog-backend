package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeEntryPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeEntryVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTriggerRequest;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeEntryAdminService;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSyncTaskAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台 AI 知识条目管理控制器。
 *
 * <p>负责知识条目的查询、状态管理和同步任务触发。
 */
@RestController
@RequestMapping("/api/sys/ai/knowledge/entries")
@Tag(name = "后台AI知识条目")
@RequiredArgsConstructor
public class AiKnowledgeEntryAdminController {

    private final AiKnowledgeEntryAdminService aiKnowledgeEntryAdminService;
    private final AiKnowledgeSyncTaskAdminService aiKnowledgeSyncTaskAdminService;

    @GetMapping
    @Operation(summary = "分页查询知识条目")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<PageResult<AiKnowledgeEntryVO>> listEntries(AiKnowledgeEntryPageQuery query) {
        return Result.success(aiKnowledgeEntryAdminService.listEntries(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询知识条目详情")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<AiKnowledgeEntryVO> getEntry(@PathVariable Long id) {
        return Result.success(aiKnowledgeEntryAdminService.getEntry(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新知识条目状态")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:update')")
    public Result<Void> updateEntryStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        Long operatorId = SecurityUtils.requireUserId();
        aiKnowledgeEntryAdminService.updateEntryStatus(id, status, operatorId);
        return Result.success();
    }

    @PostMapping("/sync")
    @Operation(summary = "触发知识同步任务")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:sync')")
    public Result<AiKnowledgeSyncTaskVO> triggerSync(
            @Valid @RequestBody AiKnowledgeSyncTriggerRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiKnowledgeSyncTaskAdminService.triggerSync(request, operatorId));
    }

    @GetMapping("/sync/tasks")
    @Operation(summary = "分页查询同步任务")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<PageResult<AiKnowledgeSyncTaskVO>> listTasks(AiKnowledgeSyncTaskPageQuery query) {
        return Result.success(aiKnowledgeSyncTaskAdminService.listTasks(query));
    }

    @GetMapping("/sync/tasks/{taskId}")
    @Operation(summary = "查询同步任务详情")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<AiKnowledgeSyncTaskVO> getTask(@PathVariable Long taskId) {
        return Result.success(aiKnowledgeSyncTaskAdminService.getTask(taskId));
    }

    @PostMapping("/sync/tasks/{taskId}/retry")
    @Operation(summary = "重试失败的同步任务")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:sync')")
    public Result<Void> retryTask(@PathVariable Long taskId) {
        Long operatorId = SecurityUtils.requireUserId();
        aiKnowledgeSyncTaskAdminService.retryTask(taskId, operatorId);
        return Result.success();
    }
}
