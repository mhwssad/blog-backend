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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 AI 调用统计控制器。
 *
 * <p>负责使用日志分页查询与统计聚合等后台运营接口。
 */
@RestController
@RequestMapping("/api/sys/ai/usage-logs")
@Tag(name = "后台AI调用统计")
@RequiredArgsConstructor
public class AiUsageLogAdminController {

    private final AiUsageLogService aiUsageLogService;

    @GetMapping
    @Operation(summary = "分页查询使用日志")
    @PreAuthorize("@permission.hasPermission('ai:log:query')")
    public Result<PageResult<AiUsageLogVO>> pageUsageLogs(AiUsageLogPageQuery query) {
        return Result.success(aiUsageLogService.pageUsageLogs(query));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取使用统计")
    @PreAuthorize("@permission.hasPermission('ai:log:query')")
    public Result<AiUsageStatsVO> getUsageStats(AiUsageLogPageQuery query) {
        return Result.success(aiUsageLogService.getUsageStats(query));
    }
}
