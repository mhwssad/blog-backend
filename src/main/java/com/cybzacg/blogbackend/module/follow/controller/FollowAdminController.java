package com.cybzacg.blogbackend.module.follow.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminPageQuery;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminRelationVO;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowRelationCleanRequest;
import com.cybzacg.blogbackend.module.follow.service.FollowAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注关系后台管理控制器。
 */
@RestController
@RequestMapping("/api/sys/follows")
@Tag(name = "关注关系后台管理")
@RequiredArgsConstructor
public class FollowAdminController {
    private final FollowAdminService followAdminService;

    @GetMapping
    @Operation(summary = "分页查询关注关系")
    @PreAuthorize("@permission.hasPermission('content:follow:query')")
    public Result<PageResult<FollowAdminRelationVO>> pageRelations(FollowAdminPageQuery query) {
        return Result.success(followAdminService.pageRelations(query));
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清理异常关注关系")
    @PreAuthorize("@permission.hasPermission('content:follow:clean')")
    public Result<Long> cleanRelations(@Valid @RequestBody FollowRelationCleanRequest request) {
        return Result.success(followAdminService.cleanRelations(request));
    }
}
