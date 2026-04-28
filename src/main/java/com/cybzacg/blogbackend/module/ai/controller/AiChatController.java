package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageSendRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiQuotaVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionCreateRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionDetailVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionVO;
import com.cybzacg.blogbackend.module.ai.service.AiChatService;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 用户侧 AI 对话控制器。
 *
 * <p>负责会话创建、列表查询、消息收发、会话关闭与额度查询等用户侧接口。
 */
@RestController
@RequestMapping("/api/user/ai/sessions")
@Tag(name = "用户AI对话")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiQuotaService aiQuotaService;

    @PostMapping
    @Operation(summary = "创建AI会话")
    public Result<AiSessionVO> createSession(@Valid @RequestBody AiSessionCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiChatService.createSession(userId, request));
    }

    @GetMapping
    @Operation(summary = "查询我的AI会话列表")
    public Result<PageResult<AiSessionVO>> listMySessions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiChatService.listMySessions(userId, current, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询AI会话详情")
    public Result<AiSessionDetailVO> getSessionDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiChatService.getSessionDetail(id, userId));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "分页查询会话消息")
    public Result<PageResult<AiMessageVO>> listMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiChatService.listMessages(id, userId, current, size));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "发送消息")
    public Result<AiMessageVO> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody AiMessageSendRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiChatService.sendMessage(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "关闭会话")
    public Result<Void> deleteSession(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        aiChatService.deleteSession(id, userId);
        return Result.success();
    }

    @GetMapping("/quota")
    @Operation(summary = "查询我的AI配额")
    public Result<AiQuotaVO> getQuota() {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(aiQuotaService.getUserQuotaForDefaultChannel(userId));
    }
}
