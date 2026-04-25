package com.cybzacg.blogbackend.module.content.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.model.admin.CommentPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.CommentStatusRequest;
import com.cybzacg.blogbackend.module.content.model.admin.CommentVO;
import com.cybzacg.blogbackend.module.content.service.CommentAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Content评论后台管理控制器。
 *
 * <p>负责对外暴露Content评论后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/comments")
@Tag(name = "后台评论管理")
@RequiredArgsConstructor
public class ContentCommentAdminController {
    private final CommentAdminService commentAdminService;

    @GetMapping
    @Operation(summary = "分页查询评论")
    @PreAuthorize("@permission.hasPermission('content:comment:query')")
    public Result<PageResult<CommentVO>> pageComments(CommentPageQuery query) {
        return Result.success(commentAdminService.pageComments(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询评论详情")
    @PreAuthorize("@permission.hasPermission('content:comment:query')")
    public Result<CommentVO> getComment(@PathVariable Long id) {
        return Result.success(commentAdminService.getComment(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改评论状态")
    @PreAuthorize("@permission.hasPermission('content:comment:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody CommentStatusRequest request) {
        commentAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    @PreAuthorize("@permission.hasPermission('content:comment:delete')")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentAdminService.deleteComment(id);
        return Result.success();
    }
}
