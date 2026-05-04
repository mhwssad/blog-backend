package com.cybzacg.blogbackend.module.forum.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.account.model.admin.StatusUpdateRequest;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionAdminVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionSaveRequest;
import com.cybzacg.blogbackend.module.forum.service.ForumSectionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛版块后台管理控制器。
 *
 * <p>负责对外暴露版块分页、详情、维护和启停删除入口。
 */
@RestController
@RequestMapping("/api/sys/forum/sections")
@Tag(name = "后台论坛版块管理")
@RequiredArgsConstructor
public class ForumSectionAdminController {
    private final ForumSectionAdminService forumSectionAdminService;

    @GetMapping
    @Operation(summary = "分页查询论坛版块")
    @PreAuthorize("@permission.hasPermission('content:forum:query')")
    public Result<PageResult<ForumSectionAdminVO>> pageSections(ForumSectionPageQuery query) {
        return Result.success(forumSectionAdminService.pageSections(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询论坛版块详情")
    @PreAuthorize("@permission.hasPermission('content:forum:query')")
    public Result<ForumSectionAdminVO> getSection(@PathVariable Long id) {
        return Result.success(forumSectionAdminService.getSection(id));
    }

    @PostMapping
    @Operation(summary = "新增论坛版块")
    @PreAuthorize("@permission.hasPermission('content:forum:create')")
    public Result<ForumSectionAdminVO> createSection(@Valid @RequestBody ForumSectionSaveRequest request) {
        return Result.success(forumSectionAdminService.createSection(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改论坛版块")
    @PreAuthorize("@permission.hasPermission('content:forum:update')")
    public Result<ForumSectionAdminVO> updateSection(@PathVariable Long id,
                                                     @Valid @RequestBody ForumSectionSaveRequest request) {
        return Result.success(forumSectionAdminService.updateSection(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改论坛版块状态")
    @PreAuthorize("@permission.hasPermission('content:forum:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        forumSectionAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除论坛版块")
    @PreAuthorize("@permission.hasPermission('content:forum:delete')")
    public Result<Void> deleteSection(@PathVariable Long id) {
        forumSectionAdminService.deleteSection(id);
        return Result.success();
    }
}
