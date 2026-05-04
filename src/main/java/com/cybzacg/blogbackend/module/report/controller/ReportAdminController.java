package com.cybzacg.blogbackend.module.report.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminPageQuery;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminVO;
import com.cybzacg.blogbackend.module.report.model.admin.ReportHandleRequest;
import com.cybzacg.blogbackend.module.report.model.admin.ReportRejectRequest;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import com.cybzacg.blogbackend.module.report.service.ReportAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台举报管理控制器。
 */
@RestController
@RequestMapping("/api/sys/reports")
@Tag(name = "后台举报管理")
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportAdminService reportAdminService;
    private final HttpServletRequest request;

    /**
     * 分页筛选举报（管理端）。
     *
     * @param query 查询条件（状态、目标类型、举报人ID、时间范围等）
     * @return 分页结果
     */
    @GetMapping
    @Operation(
        summary = "分页筛选举报",
        description = "管理端分页查询举报列表，支持按状态、目标类型、举报人ID、时间范围等条件筛选。"
    )
    @PreAuthorize("@permission.hasPermission('sys:report:query')")
    public Result<PageResult<ReportAdminVO>> pageReports(
        ReportAdminPageQuery query
    ) {
        return Result.success(reportAdminService.pageReports(query));
    }

    /**
     * 举报详情（管理端）。
     *
     * @param id 举报记录ID
     * @return 举报详情VO
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "举报详情",
        description = "查询单条举报记录的完整详情，包含处理人、被举报对象信息。"
    )
    @PreAuthorize("@permission.hasPermission('sys:report:query')")
    public Result<ReportAdminVO> getReportDetail(@PathVariable Long id) {
        return Result.success(reportAdminService.getReportDetail(id));
    }

    /**
     * 接手（认领）举报。
     * 将待处理状态的举报标记为处理中，记录当前操作人为处理人。
     *
     * @param id 举报记录ID
     * @return void
     */
    @PutMapping("/{id}/take")
    @Operation(
        summary = "接手举报",
        description = "将待处理的举报标记为处理中，记录当前操作人为处理人。" +
            "仅在举报状态为待处理（PENDING）时可操作。"
    )
    @PreAuthorize("@permission.hasPermission('sys:report:handle')")
    public Result<Void> claimReport(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        reportAdminService.claimReport(id, operatorId);
        return Result.success();
    }

    /**
     * 处理举报。
     * 更新状态为已处理，写入处理结果与处罚类型，执行业务治理动作并记录审计日志。
     *
     * @param id           举报记录ID
     * @param handleRequest 处理请求（resultType必填，punishmentType/remark可选）
     * @return void
     */
    @PutMapping("/{id}/handle")
    @Operation(
        summary = "处理举报",
        description = "对举报进行处理并执行对应治理动作（删除内容、撤回消息、禁言、封禁等）。" +
            "处理后将状态更新为已处理，记录审计日志。"
    )
    @PreAuthorize("@permission.hasPermission('sys:report:handle')")
    public Result<Void> handleReport(
        @PathVariable Long id,
        @Valid @RequestBody ReportHandleRequest handleRequest
    ) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        reportAdminService.handleReport(id, operatorId, handleRequest, ip, ua);
        return Result.success();
    }

    /**
     * 驳回举报。
     *
     * @param id             举报记录ID
     * @param rejectRequest  驳回请求（remark必填）
     * @return void
     */
    @PutMapping("/{id}/reject")
    @Operation(
        summary = "驳回举报",
        description = "驳回无效举报，将状态标记为已驳回并记录驳回备注。"
    )
    @PreAuthorize("@permission.hasPermission('sys:report:handle')")
    public Result<Void> rejectReport(
        @PathVariable Long id,
        @RequestBody ReportRejectRequest rejectRequest
    ) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        reportAdminService.rejectReport(
            id,
            operatorId,
            rejectRequest.getRemark(),
            ip,
            ua
        );
        return Result.success();
    }

    /**
     * 超管接管举报。
     * 仅超级管理员可操作，强占当前处理人并重新认领。
     *
     * @param id 举报记录ID
     * @return void
     */
    @PutMapping("/{id}/override")
    @Operation(
        summary = "超管接管举报",
        description = "超级管理员强制接管正在处理中的举报，重新认领并记录原处理人。" +
            "已处理或已驳回的举报不可再接管。"
    )
    @PreAuthorize("@permission.hasPermission('sys:report:handle')")
    public Result<Void> overrideClaim(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        reportAdminService.overrideClaim(id, operatorId, ip, ua);
        return Result.success();
    }

    /**
     * 处理日志。
     *
     * @param id 举报记录ID
     * @return 处理日志列表（按时间倒序）
     */
    @GetMapping("/{id}/logs")
    @Operation(
        summary = "处理日志",
        description = "查询某条举报记录的所有处理操作日志，包含认领、处理、驳回、超管接管等操作记录。"
    )
    @PreAuthorize("@permission.hasPermission('sys:report:query')")
    public Result<List<ReportHandleLogVO>> listHandleLogs(
        @PathVariable Long id
    ) {
        return Result.success(reportAdminService.listHandleLogs(id));
    }
}
