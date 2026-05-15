package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminVO;
import com.cybzacg.blogbackend.module.ai.service.AiAgentTaskAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 AI Agent 任务管理控制器。
 *
 * <p>提供管理员分页查询与查看 Agent 任务详情的后台运营接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/ai/agents/tasks")
@Tag(name = "后台AI Agent任务管理")
@RequiredArgsConstructor
public class AiAgentTaskAdminController {

    private final AiAgentTaskAdminService aiAgentTaskAdminService;

    /**
     * 分页查询所有 Agent 任务列表（后台视角）。
     *
     * @param query 分页查询参数，包含页码、页大小及可选筛选条件
     * @return 分页包装的 Agent 任务后台视图对象
     */
    @GetMapping
    @Operation(summary = "分页查询 Agent 任务")
    @PreAuthorize("@permission.hasPermission('ai:agent:query')")
    public Result<PageResult<AiAgentTaskAdminVO>> pageTasks(AiAgentTaskAdminPageQuery query) {
        log.debug("后台分页查询Agent任务: query={}", query);
        return Result.success(aiAgentTaskAdminService.pageTasks(query));
    }

    /**
     * 查询指定 Agent 任务的详情（后台视角）。
     *
     * @param id Agent 任务主键ID
     * @return Agent 任务后台视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询 Agent 任务详情")
    @PreAuthorize("@permission.hasPermission('ai:agent:query')")
    public Result<AiAgentTaskAdminVO> getTask(@PathVariable Long id) {
        log.debug("后台查询Agent任务详情: taskId={}", id);
        return Result.success(aiAgentTaskAdminService.getTask(id));
    }
}
