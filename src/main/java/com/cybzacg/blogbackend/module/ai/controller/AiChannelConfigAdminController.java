package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelStatusRequest;
import com.cybzacg.blogbackend.module.ai.service.AiChannelConfigAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台 AI 渠道配置管理控制器。
 *
 * <p>负责渠道配置的增删改查及状态管理等后台运营接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/ai/channels")
@Tag(name = "后台AI渠道配置")
@RequiredArgsConstructor
public class AiChannelConfigAdminController {

    private final AiChannelConfigAdminService aiChannelConfigAdminService;

    /**
     * 分页查询渠道配置列表。
     *
     * @param current 当前页码，默认为 1
     * @param size    每页条数，默认为 10
     * @return 分页包装的渠道配置视图对象
     */
    @GetMapping
    @Operation(summary = "分页查询渠道配置")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:query')")
    public Result<PageResult<AiChannelConfigVO>> listChannels(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        log.debug("后台分页查询AI渠道配置: current={}, size={}", current, size);
        return Result.success(aiChannelConfigAdminService.listChannels(current, size));
    }

    /**
     * 查询指定渠道配置的详情。
     *
     * @param id 渠道配置主键ID
     * @return 渠道配置视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询渠道配置详情")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:query')")
    public Result<AiChannelConfigVO> getChannel(@PathVariable Long id) {
        log.debug("后台查询AI渠道配置详情: channelId={}", id);
        return Result.success(aiChannelConfigAdminService.getChannel(id));
    }

    /**
     * 创建新的渠道配置。
     *
     * @param request 渠道配置保存请求体
     * @return 创建后的渠道配置视图对象
     */
    @PostMapping
    @Operation(summary = "创建渠道配置")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:create')")
    public Result<AiChannelConfigVO> createChannel(@Valid @RequestBody AiChannelConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台创建AI渠道配置: operatorId={}, request={}", operatorId, request);
        return Result.success(aiChannelConfigAdminService.createChannel(request, operatorId));
    }

    /**
     * 更新指定渠道配置。
     *
     * @param id      渠道配置主键ID
     * @param request 渠道配置保存请求体
     * @return 更新后的渠道配置视图对象
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新渠道配置")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:update')")
    public Result<AiChannelConfigVO> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody AiChannelConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台更新AI渠道配置: channelId={}, operatorId={}", id, operatorId);
        return Result.success(aiChannelConfigAdminService.updateChannel(id, request, operatorId));
    }

    /**
     * 更新渠道启用/禁用状态。
     *
     * @param id      渠道配置主键ID
     * @param request 状态更新请求体，包含目标状态值
     * @return 空结果
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新渠道状态")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:update')")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AiChannelStatusRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台更新AI渠道状态: channelId={}, status={}, operatorId={}", id, request.getStatus(), operatorId);
        aiChannelConfigAdminService.updateStatus(id, request.getStatus(), operatorId);
        return Result.success();
    }

    /**
     * 删除指定渠道配置。
     *
     * @param id 渠道配置主键ID
     * @return 空结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除渠道配置")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:delete')")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台删除AI渠道配置: channelId={}, operatorId={}", id, operatorId);
        aiChannelConfigAdminService.deleteChannel(id, operatorId);
        return Result.success();
    }
}
