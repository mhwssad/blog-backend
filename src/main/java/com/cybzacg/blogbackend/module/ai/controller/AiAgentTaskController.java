package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskCreateRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskVO;
import com.cybzacg.blogbackend.module.ai.service.AiAgentTaskService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 AI Agent 任务控制器。
 */
@RestController
@RequestMapping("/api/user/ai/agents/tasks")
@Tag(name = "用户AI Agent任务")
@RequiredArgsConstructor
public class AiAgentTaskController {

    private final AiAgentTaskService aiAgentTaskService;

    @PostMapping
    @Operation(summary = "发起 Agent 任务")
    public Result<AiAgentTaskVO> createTask(@Valid @RequestBody AiAgentTaskCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiAgentTaskService.createTask(userId, request));
    }

    @GetMapping
    @Operation(summary = "分页查询我的 Agent 任务")
    public Result<PageResult<AiAgentTaskVO>> pageMyTasks(AiAgentTaskPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiAgentTaskService.pageMyTasks(userId, query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询 Agent 任务详情")
    public Result<AiAgentTaskVO> getTask(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiAgentTaskService.getTask(userId, id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消 Agent 任务")
    public Result<Void> cancelTask(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        aiAgentTaskService.cancelTask(userId, id);
        return Result.success();
    }
}
