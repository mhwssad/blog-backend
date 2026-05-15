package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.user.*;
import com.cybzacg.blogbackend.module.ai.service.AiChatService;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


/**
 * 用户侧 AI 对话控制器。
 *
 * <p>负责会话创建、列表查询、消息收发、会话关闭与额度查询等用户侧接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/ai/sessions")
@Tag(name = "用户AI对话")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiQuotaService aiQuotaService;

    /**
     * 创建新的 AI 会话。
     *
     * @param request 会话创建请求体，包含会话标题等参数
     * @return 创建后的会话视图对象
     */
    @PostMapping
    @Operation(summary = "创建AI会话")
    public Result<AiSessionVO> createSession(@Valid @RequestBody AiSessionCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        log.info("用户创建AI会话: userId={}", userId);
        return Result.success(aiChatService.createSession(userId, request));
    }

    /**
     * 分页查询当前用户的 AI 会话列表。
     *
     * @param current 当前页码，默认为 1
     * @param size    每页条数，默认为 10
     * @return 分页包装的会话视图对象
     */
    @GetMapping
    @Operation(summary = "查询我的AI会话列表")
    public Result<PageResult<AiSessionVO>> listMySessions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Long userId = SecurityUtils.requireUserId();
        log.debug("用户查询AI会话列表: userId={}, current={}, size={}", userId, current, size);
        return Result.success(aiChatService.listMySessions(userId, current, size));
    }

    /**
     * 查询指定 AI 会话的详情。
     *
     * @param id 会话主键ID
     * @return 会话详情视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询AI会话详情")
    public Result<AiSessionDetailVO> getSessionDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        log.debug("用户查询AI会话详情: userId={}, sessionId={}", userId, id);
        return Result.success(aiChatService.getSessionDetail(id, userId));
    }

    /**
     * 分页查询指定会话的历史消息。
     *
     * @param id      会话主键ID
     * @param current 当前页码，默认为 1
     * @param size    每页条数，默认为 20
     * @return 分页包装的消息视图对象
     */
    @GetMapping("/{id}/messages")
    @Operation(summary = "分页查询会话消息")
    public Result<PageResult<AiMessageVO>> listMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        Long userId = SecurityUtils.requireUserId();
        log.debug("用户分页查询会话消息: userId={}, sessionId={}, current={}, size={}", userId, id, current, size);
        return Result.success(aiChatService.listMessages(id, userId, current, size));
    }

    /**
     * 向指定会话发送消息（同步模式）。
     *
     * @param id      会话主键ID
     * @param request 消息发送请求体，包含消息内容
     * @return AI 回复消息视图对象
     */
    @PostMapping("/{id}/messages")
    @Operation(summary = "发送消息")
    public Result<AiMessageVO> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody AiMessageSendRequest request) {
        Long userId = SecurityUtils.requireUserId();
        log.info("用户发送AI消息: userId={}, sessionId={}", userId, id);
        return Result.success(aiChatService.sendMessage(id, userId, request));
    }

    /**
     * 向指定会话发送消息并以 SSE 流式方式返回 AI 回复。
     *
     * @param id      会话主键ID
     * @param request 消息发送请求体，包含消息内容
     * @return SSE 发射器，用于流式推送 AI 回复
     */
    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送消息（SSE）")
    public SseEmitter streamMessage(
            @PathVariable Long id,
            @Valid @RequestBody AiMessageSendRequest request) {
        Long userId = SecurityUtils.requireUserId();
        log.info("用户流式发送AI消息: userId={}, sessionId={}", userId, id);
        return aiChatService.streamMessage(id, userId, request);
    }

    /**
     * 关闭（删除）指定的 AI 会话。
     *
     * @param id 会话主键ID
     * @return 空结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "关闭会话")
    public Result<Void> deleteSession(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        log.info("用户关闭AI会话: userId={}, sessionId={}", userId, id);
        aiChatService.deleteSession(id, userId);
        return Result.success();
    }

    /**
     * 查询当前用户在默认渠道下的 AI 配额信息。
     *
     * @return AI 配额视图对象
     */
    @GetMapping("/quota")
    @Operation(summary = "查询我的AI配额")
    public Result<AiQuotaVO> getQuota() {
        Long userId = SecurityUtils.requireUserId();
        log.debug("用户查询AI配额: userId={}", userId);
        return Result.success(aiQuotaService.getUserQuotaForDefaultChannel(userId));
    }
}
