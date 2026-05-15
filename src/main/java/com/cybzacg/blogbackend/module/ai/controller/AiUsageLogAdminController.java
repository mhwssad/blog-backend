package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageStatsVO;
import com.cybzacg.blogbackend.module.ai.service.AiUsageLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 AI 调用统计控制器。
 *
 * <p>负责使用日志分页查询与统计聚合等后台运营接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/ai/usage-logs")
@Tag(name = "后台AI调用统计")
@RequiredArgsConstructor
public class AiUsageLogAdminController {

    private final AiUsageLogService aiUsageLogService;

    /**
     * 分页查询 AI 使用日志。
     *
     * @param query 分页查询参数，包含页码、页大小及可选的筛选条件
     * @return 分页包装的使用日志视图对象
     */
    @GetMapping
    @Operation(summary = "分页查询使用日志")
    @PreAuthorize("@permission.hasPermission('ai:usage-stats:query')")
    public Result<PageResult<AiUsageLogVO>> pageUsageLogs(AiUsageLogPageQuery query) {
        log.debug("后台分页查询AI使用日志: query={}", query);
        return Result.success(aiUsageLogService.pageUsageLogs(query));
    }

    /**
     * 获取 AI 使用统计聚合数据。
     *
     * @param query 统计查询参数，用于指定统计维度与筛选条件
     * @return 使用统计视图对象
     */
    @GetMapping("/stats")
    @Operation(summary = "获取使用统计")
    @PreAuthorize("@permission.hasPermission('ai:usage-stats:query')")
    public Result<AiUsageStatsVO> getUsageStats(AiUsageLogPageQuery query) {
        log.debug("后台查询AI使用统计: query={}", query);
        return Result.success(aiUsageLogService.getUsageStats(query));
    }
}
