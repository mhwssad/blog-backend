package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionAdminVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionPageQuery;
import com.cybzacg.blogbackend.module.ai.service.AiChatAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/sys/ai/sessions")
@Tag(name = "后台AI会话管理")
@RequiredArgsConstructor
public class AiChatAdminController {

    private final AiChatAdminService aiChatAdminService;

    /**
     * 分页查询用户 AI 会话列表。
     *
     * @param query 分页查询参数，包含页码、页大小及可选筛选条件
     * @return 分页包装的会话管理视图对象
     */
    @GetMapping
    @Operation(summary = "分页查询用户会话")
    @PreAuthorize("@permission.hasPermission('ai:session:query')")
    public Result<PageResult<AiSessionAdminVO>> pageSessions(AiSessionPageQuery query) {
        log.debug("后台分页查询AI会话: query={}", query);
        return Result.success(aiChatAdminService.pageSessions(query));
    }

    /**
     * 查询指定会话的详情信息。
     *
     * @param id 会话主键ID
     * @return 会话管理视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询会话详情")
    @PreAuthorize("@permission.hasPermission('ai:session:query')")
    public Result<AiSessionAdminVO> getSessionDetail(@PathVariable Long id) {
        log.debug("后台查询AI会话详情: sessionId={}", id);
        return Result.success(aiChatAdminService.getSessionDetail(id));
    }
}
