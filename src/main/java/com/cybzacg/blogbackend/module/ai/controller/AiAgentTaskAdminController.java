package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminVO;
import com.cybzacg.blogbackend.module.ai.service.AiAgentTaskAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 AI Agent 任务管理控制器。
 */
@RestController
@RequestMapping("/api/sys/ai/agents/tasks")
@Tag(name = "后台AI Agent任务管理")
@RequiredArgsConstructor
public class AiAgentTaskAdminController {

    private final AiAgentTaskAdminService aiAgentTaskAdminService;

    @GetMapping
    @Operation(summary = "分页查询 Agent 任务")
    @PreAuthorize("@permission.hasPermission('ai:agent:query')")
    public Result<PageResult<AiAgentTaskAdminVO>> pageTasks(AiAgentTaskAdminPageQuery query) {
        return Result.success(aiAgentTaskAdminService.pageTasks(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询 Agent 任务详情")
    @PreAuthorize("@permission.hasPermission('ai:agent:query')")
    public Result<AiAgentTaskAdminVO> getTask(@PathVariable Long id) {
        return Result.success(aiAgentTaskAdminService.getTask(id));
    }
}
