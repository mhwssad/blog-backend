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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 AI Agent 任务控制器。
 *
 * <p>提供用户发起 Agent 任务、查询任务列表与详情、取消任务等用户侧接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/ai/agents/tasks")
@Tag(name = "用户AI Agent任务")
@RequiredArgsConstructor
public class AiAgentTaskController {

    private final AiAgentTaskService aiAgentTaskService;

    /**
     * 发起新的 Agent 任务。
     *
     * @param request Agent 任务创建请求体，包含任务参数
     * @return 创建后的 Agent 任务视图对象
     */
    @PostMapping
    @Operation(summary = "发起 Agent 任务")
    public Result<AiAgentTaskVO> createTask(@Valid @RequestBody AiAgentTaskCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        log.info("用户发起Agent任务: userId={}", userId);
        return Result.success(aiAgentTaskService.createTask(userId, request));
    }

    /**
     * 分页查询当前用户的 Agent 任务列表。
     *
     * @param query 分页查询参数，包含页码、页大小及可选筛选条件
     * @return 分页包装的 Agent 任务视图对象
     */
    @GetMapping
    @Operation(summary = "分页查询我的 Agent 任务")
    public Result<PageResult<AiAgentTaskVO>> pageMyTasks(AiAgentTaskPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        log.debug("用户分页查询Agent任务: userId={}, query={}", userId, query);
        return Result.success(aiAgentTaskService.pageMyTasks(userId, query));
    }

    /**
     * 查询指定 Agent 任务的详情。
     *
     * @param id Agent 任务主键ID
     * @return Agent 任务视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询 Agent 任务详情")
    public Result<AiAgentTaskVO> getTask(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        log.debug("用户查询Agent任务详情: userId={}, taskId={}", userId, id);
        return Result.success(aiAgentTaskService.getTask(userId, id));
    }

    /**
     * 取消进行中的 Agent 任务。
     *
     * @param id Agent 任务主键ID
     * @return 空结果
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消 Agent 任务")
    public Result<Void> cancelTask(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        log.info("用户取消Agent任务: userId={}, taskId={}", userId, id);
        aiAgentTaskService.cancelTask(userId, id);
        return Result.success();
    }
}
