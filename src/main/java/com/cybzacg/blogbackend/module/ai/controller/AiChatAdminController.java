package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionAdminVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionPageQuery;
import com.cybzacg.blogbackend.module.ai.service.AiChatAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 AI 会话管理控制器。
 *
 * <p>提供管理员查询用户 AI 会话列表与详情的后台接口。
 */
@RestController
@RequestMapping("/api/sys/ai/sessions")
@Tag(name = "后台AI会话管理")
@RequiredArgsConstructor
public class AiChatAdminController {

    private final AiChatAdminService aiChatAdminService;

    @GetMapping
    @Operation(summary = "分页查询用户会话")
    @PreAuthorize("@permission.hasPermission('ai:session:query')")
    public Result<PageResult<AiSessionAdminVO>> pageSessions(AiSessionPageQuery query) {
        return Result.success(aiChatAdminService.pageSessions(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询会话详情")
    @PreAuthorize("@permission.hasPermission('ai:session:query')")
    public Result<AiSessionAdminVO> getSessionDetail(@PathVariable Long id) {
        return Result.success(aiChatAdminService.getSessionDetail(id));
    }
}
