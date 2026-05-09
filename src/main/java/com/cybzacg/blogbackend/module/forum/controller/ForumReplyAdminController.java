package com.cybzacg.blogbackend.module.forum.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminVO;
import com.cybzacg.blogbackend.module.forum.service.ForumReplyAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛回复后台管理控制器。
 */
@RestController
@RequestMapping("/api/sys/forum/replies")
@Tag(name = "后台论坛回复管理")
@RequiredArgsConstructor
@Validated
public class ForumReplyAdminController {
    private final ForumReplyAdminService forumReplyAdminService;

    @GetMapping
    @Operation(summary = "分页查询论坛回复")
    @PreAuthorize("@permission.hasPermission('content:forum:query')")
    public Result<PageResult<ForumReplyAdminVO>> pageReplies(ForumReplyAdminPageQuery query) {
        return Result.success(forumReplyAdminService.pageReplies(query));
    }

    @PutMapping("/{id}/hide")
    @Operation(summary = "隐藏论坛回复")
    @PreAuthorize("@permission.hasPermission('content:forum:update')")
    public Result<Void> hideReply(@PathVariable @NotNull @Positive Long id, HttpServletRequest httpRequest) {
        forumReplyAdminService.hideReply(id, operatorId(httpRequest), ip(httpRequest), ua(httpRequest));
        return Result.success();
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "恢复论坛回复")
    @PreAuthorize("@permission.hasPermission('content:forum:update')")
    public Result<Void> restoreReply(@PathVariable @NotNull @Positive Long id, HttpServletRequest httpRequest) {
        forumReplyAdminService.restoreReply(id, operatorId(httpRequest), ip(httpRequest), ua(httpRequest));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除论坛回复")
    @PreAuthorize("@permission.hasPermission('content:forum:delete')")
    public Result<Void> deleteReply(@PathVariable @NotNull @Positive Long id, HttpServletRequest httpRequest) {
        forumReplyAdminService.deleteReply(id, operatorId(httpRequest), ip(httpRequest), ua(httpRequest));
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
