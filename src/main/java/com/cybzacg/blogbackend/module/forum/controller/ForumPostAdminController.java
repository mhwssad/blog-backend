package com.cybzacg.blogbackend.module.forum.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminDetailVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminVO;
import com.cybzacg.blogbackend.module.forum.service.ForumPostAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛帖子后台管理控制器。
 */
@RestController
@RequestMapping("/api/sys/forum/posts")
@Tag(name = "后台论坛帖子管理")
@RequiredArgsConstructor
public class ForumPostAdminController {
    private final ForumPostAdminService forumPostAdminService;

    @GetMapping
    @Operation(summary = "分页查询论坛帖子")
    @PreAuthorize("@permission.hasPermission('content:forum:query')")
    public Result<PageResult<ForumPostAdminVO>> pagePosts(ForumPostAdminPageQuery query) {
        return Result.success(forumPostAdminService.pagePosts(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询论坛帖子详情")
    @PreAuthorize("@permission.hasPermission('content:forum:query')")
    public Result<ForumPostAdminDetailVO> getPost(@PathVariable Long id) {
        return Result.success(forumPostAdminService.getPost(id));
    }

    @PutMapping("/{id}/hide")
    @Operation(summary = "隐藏论坛帖子")
    @PreAuthorize("@permission.hasPermission('content:forum:update')")
    public Result<Void> hidePost(@PathVariable Long id, HttpServletRequest httpRequest) {
        forumPostAdminService.hidePost(id, operatorId(httpRequest), ip(httpRequest), ua(httpRequest));
        return Result.success();
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "恢复论坛帖子")
    @PreAuthorize("@permission.hasPermission('content:forum:update')")
    public Result<Void> restorePost(@PathVariable Long id, HttpServletRequest httpRequest) {
        forumPostAdminService.restorePost(id, operatorId(httpRequest), ip(httpRequest), ua(httpRequest));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除论坛帖子")
    @PreAuthorize("@permission.hasPermission('content:forum:delete')")
    public Result<Void> deletePost(@PathVariable Long id, HttpServletRequest httpRequest) {
        forumPostAdminService.deletePost(id, operatorId(httpRequest), ip(httpRequest), ua(httpRequest));
        return Result.success();
    }

    @PutMapping("/{id}/top")
    @Operation(summary = "切换论坛帖子置顶")
    @PreAuthorize("@permission.hasPermission('content:forum:update')")
    public Result<Void> toggleTop(@PathVariable Long id,
                                  @RequestParam boolean enabled,
                                  HttpServletRequest httpRequest) {
        forumPostAdminService.toggleTop(id, enabled, operatorId(httpRequest), ip(httpRequest), ua(httpRequest));
        return Result.success();
    }

    @PutMapping("/{id}/essence")
    @Operation(summary = "切换论坛帖子精华")
    @PreAuthorize("@permission.hasPermission('content:forum:update')")
    public Result<Void> toggleEssence(@PathVariable Long id,
                                      @RequestParam boolean enabled,
                                      HttpServletRequest httpRequest) {
        forumPostAdminService.toggleEssence(id, enabled, operatorId(httpRequest), ip(httpRequest), ua(httpRequest));
        return Result.success();
    }

    private Long operatorId(HttpServletRequest request) {
        return SecurityUtils.requireUserId();
    }

    private String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String ua(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
