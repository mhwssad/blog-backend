package com.cybzacg.blogbackend.module.report.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.report.model.user.ReportCreateRequest;
import com.cybzacg.blogbackend.module.report.model.user.ReportVO;
import com.cybzacg.blogbackend.module.report.service.ReportService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户侧举报控制器。
 */
@RestController
@RequestMapping("/api/user/reports")
@Tag(name = "用户举报")
@RequiredArgsConstructor
@Validated
public class UserReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "提交举报")
    public Result<ReportVO> submitReport(
        @Valid @RequestBody ReportCreateRequest request
    ) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(reportService.submitReport(userId, request));
    }

    @GetMapping
    @Operation(summary = "查询我的举报记录")
    public Result<PageResult<ReportVO>> listMyReports(
        @RequestParam(required = false) String targetType,
        @RequestParam(defaultValue = "1") @NotNull @Min(1) Long current,
        @RequestParam(defaultValue = "10") @NotNull @Min(1) Long size
    ) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(
            reportService.listMyReports(userId, targetType, current, size)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询举报详情")
    public Result<ReportVO> getMyReport(@PathVariable @NotNull @Positive Long id) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(reportService.getMyReport(userId, id));
    }
}
