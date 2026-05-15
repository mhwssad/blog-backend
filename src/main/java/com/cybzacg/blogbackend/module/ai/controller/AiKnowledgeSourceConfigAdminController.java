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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台 AI 知识源配置管理控制器。
 *
 * <p>负责知识源配置的查询、更新和启停管理。
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/ai/knowledge/source-config")
@Tag(name = "后台AI知识源配置")
@RequiredArgsConstructor
public class AiKnowledgeSourceConfigAdminController {

    private final AiKnowledgeSourceConfigAdminService aiKnowledgeSourceConfigAdminService;

    /**
     * 查询所有知识源配置列表。
     *
     * @return 知识源配置视图对象列表
     */
    @GetMapping
    @Operation(summary = "查询所有知识源配置")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<List<AiKnowledgeSourceConfigVO>> listConfigs() {
        log.debug("后台查询所有知识源配置");
        return Result.success(aiKnowledgeSourceConfigAdminService.listConfigs());
    }

    /**
     * 查询指定知识源配置的详情。
     *
     * @param id 知识源配置主键ID
     * @return 知识源配置视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询知识源配置详情")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:query')")
    public Result<AiKnowledgeSourceConfigVO> getConfig(@PathVariable Long id) {
        log.debug("后台查询知识源配置详情: configId={}", id);
        return Result.success(aiKnowledgeSourceConfigAdminService.getConfig(id));
    }

    /**
     * 更新指定知识源配置。
     *
     * @param id      知识源配置主键ID
     * @param request 知识源配置保存请求体
     * @return 更新后的知识源配置视图对象
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识源配置")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:update')")
    public Result<AiKnowledgeSourceConfigVO> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody AiKnowledgeSourceConfigSaveRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台更新知识源配置: configId={}, operatorId={}", id, operatorId);
        return Result.success(aiKnowledgeSourceConfigAdminService.updateConfig(id, request, operatorId));
    }

    /**
     * 切换知识源的启用/禁用状态。
     *
     * @param id      知识源配置主键ID
     * @param enabled 目标状态值（1-启用，0-禁用）
     * @return 空结果
     */
    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换知识源启停状态")
    @PreAuthorize("@permission.hasPermission('ai:knowledge:update')")
    public Result<Void> toggleEnabled(
            @PathVariable Long id,
            @RequestParam Integer enabled) {
        Long operatorId = SecurityUtils.requireUserId();
        log.info("后台切换知识源启停状态: configId={}, enabled={}, operatorId={}", id, enabled, operatorId);
        aiKnowledgeSourceConfigAdminService.toggleEnabled(id, enabled, operatorId);
        return Result.success();
    }
}
