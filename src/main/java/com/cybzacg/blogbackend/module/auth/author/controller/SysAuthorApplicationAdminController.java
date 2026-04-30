package com.cybzacg.blogbackend.module.auth.author.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminReviewRequest;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminVO;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationRepairRequest;
import com.cybzacg.blogbackend.module.auth.author.service.SysAuthorApplicationAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 作者申请后台管理控制器。
 */
@RestController
@RequestMapping("/api/sys/author-applications")
@Tag(name = "作者申请后台管理")
@RequiredArgsConstructor
public class SysAuthorApplicationAdminController {
    private final SysAuthorApplicationAdminService sysAuthorApplicationAdminService;

    @GetMapping
    @Operation(summary = "分页查询作者申请")
    @PreAuthorize("@permission.hasPermission('sys:author-application:query')")
    public Result<PageResult<SysAuthorApplicationAdminVO>> pageApplications(SysAuthorApplicationAdminPageQuery query) {
        return Result.success(sysAuthorApplicationAdminService.pageApplications(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询作者申请详情")
    @PreAuthorize("@permission.hasPermission('sys:author-application:query')")
    public Result<SysAuthorApplicationAdminVO> getApplication(@PathVariable Long id) {
        return Result.success(sysAuthorApplicationAdminService.getApplication(id));
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "审核作者申请")
    @PreAuthorize("@permission.hasPermission('sys:author-application:review')")
    public Result<Void> reviewApplication(@PathVariable Long id,
                                          @Valid @RequestBody SysAuthorApplicationAdminReviewRequest request) {
        sysAuthorApplicationAdminService.reviewApplication(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/repair")
    @Operation(summary = "修正作者申请状态")
    @PreAuthorize("@permission.hasPermission('sys:author-application:repair')")
    public Result<Void> repairApplication(@PathVariable Long id,
                                          @Valid @RequestBody SysAuthorApplicationRepairRequest request) {
        sysAuthorApplicationAdminService.repairApplication(id, request);
        return Result.success();
    }
}
