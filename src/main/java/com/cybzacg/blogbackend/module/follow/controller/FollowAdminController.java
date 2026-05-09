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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 关注关系后台管理控制器。
 *
 * <p>负责关注关系的分页查询与异常数据清理等后台运营接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/follows")
@Tag(name = "关注关系后台管理")
@RequiredArgsConstructor
@Validated
public class FollowAdminController {
    private final FollowAdminService followAdminService;

    @GetMapping
    @Operation(summary = "分页查询关注关系")
    @PreAuthorize("@permission.hasPermission('content:follow:query')")
    public Result<PageResult<FollowAdminRelationVO>> pageRelations(FollowAdminPageQuery query) {
        log.info("开始分页查询关注关系, query={}", query);
        return Result.success(followAdminService.pageRelations(query));
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清理异常关注关系")
    @PreAuthorize("@permission.hasPermission('content:follow:clean')")
    public Result<Long> cleanRelations(@Valid @RequestBody FollowRelationCleanRequest request) {
        log.info("开始清理异常关注关系, request={}", request);
        return Result.success(followAdminService.cleanRelations(request));
    }
}
