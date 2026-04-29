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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 AI 渠道配置管理控制器。
 *
 * <p>负责渠道配置的增删改查及状态管理等后台运营接口。
 */
@RestController
@RequestMapping("/api/sys/ai/channels")
@Tag(name = "后台AI渠道配置")
@RequiredArgsConstructor
public class AiChannelConfigAdminController {

    private final AiChannelConfigAdminService aiChannelConfigAdminService;

    @GetMapping
    @Operation(summary = "分页查询渠道配置")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:query')")
    public Result<PageResult<AiChannelConfigVO>> listChannels(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(aiChannelConfigAdminService.listChannels(current, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询渠道配置详情")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:query')")
    public Result<AiChannelConfigVO> getChannel(@PathVariable Long id) {
        return Result.success(aiChannelConfigAdminService.getChannel(id));
    }

    @PostMapping
    @Operation(summary = "创建渠道配置")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:create')")
    public Result<AiChannelConfigVO> createChannel(@Valid @RequestBody AiChannelConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiChannelConfigAdminService.createChannel(request, operatorId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新渠道配置")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:update')")
    public Result<AiChannelConfigVO> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody AiChannelConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiChannelConfigAdminService.updateChannel(id, request, operatorId));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新渠道状态")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:update')")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AiChannelStatusRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        aiChannelConfigAdminService.updateStatus(id, request.getStatus(), operatorId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除渠道配置")
    @PreAuthorize("@permission.hasPermission('ai:channel-config:delete')")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        aiChannelConfigAdminService.deleteChannel(id, operatorId);
        return Result.success();
    }
}
