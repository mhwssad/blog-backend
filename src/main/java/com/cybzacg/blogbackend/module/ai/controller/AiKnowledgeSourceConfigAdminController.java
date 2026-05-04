package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigVO;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSourceConfigAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台 AI 知识源配置管理控制器。
 *
 * <p>负责知识源配置的查询、更新和启停管理。
 */
@RestController
@RequestMapping("/api/sys/ai/knowledge/source-config")
@Tag(name = "后台AI知识源配置")
@RequiredArgsConstructor
public class AiKnowledgeSourceConfigAdminController {

    private final AiKnowledgeSourceConfigAdminService aiKnowledgeSourceConfigAdminService;

    @GetMapping
    @Operation(summary = "查询所有知识源配置")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<List<AiKnowledgeSourceConfigVO>> listConfigs() {
        return Result.success(aiKnowledgeSourceConfigAdminService.listConfigs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询知识源配置详情")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<AiKnowledgeSourceConfigVO> getConfig(@PathVariable Long id) {
        return Result.success(aiKnowledgeSourceConfigAdminService.getConfig(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识源配置")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:update')")
    public Result<AiKnowledgeSourceConfigVO> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody AiKnowledgeSourceConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(aiKnowledgeSourceConfigAdminService.updateConfig(id, request, operatorId));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换知识源启停状态")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:update')")
    public Result<Void> toggleEnabled(
            @PathVariable Long id,
            @RequestParam Integer enabled) {
        Long operatorId = SecurityUtils.requireUserId();
        aiKnowledgeSourceConfigAdminService.toggleEnabled(id, enabled, operatorId);
        return Result.success();
    }
}
