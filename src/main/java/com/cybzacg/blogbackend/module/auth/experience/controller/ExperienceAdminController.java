package com.cybzacg.blogbackend.module.auth.experience.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.experience.model.admin.*;
import com.cybzacg.blogbackend.module.auth.experience.service.ExperienceAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 经验体系管理端接口。
 */
@RestController
@RequestMapping("/api/sys/experience")
@Tag(name = "经验体系管理")
@RequiredArgsConstructor
public class ExperienceAdminController {

    private final ExperienceAdminService experienceAdminService;

    @GetMapping("/users/{userId}/summary")
    @Operation(summary = "查看用户经验来源汇总")
    @PreAuthorize("@permission.hasPermission('sys:experience:query')")
    public Result<UserExperienceSummaryVO> getUserExperienceSummary(@PathVariable Long userId) {
        return Result.success(experienceAdminService.getUserExperienceSummary(userId));
    }

    @GetMapping("/logs")
    @Operation(summary = "经验流水分页查询")
    @PreAuthorize("@permission.hasPermission('sys:experience:query')")
    public Result<PageResult<ExperienceLogVO>> pageExperienceLogs(ExperienceLogPageQuery query) {
        return Result.success(experienceAdminService.pageExperienceLogs(query));
    }

    @PostMapping("/users/{userId}/adjust")
    @Operation(summary = "手动调整等级或经验")
    @PreAuthorize("@permission.hasPermission('sys:experience:adjust')")
    public Result<Void> adjustUserLevelOrExperience(@PathVariable Long userId,
                                                     @Valid @RequestBody UserLevelAdjustRequest request) {
        experienceAdminService.adjustUserLevelOrExperience(userId, request);
        return Result.success();
    }

    @GetMapping("/config")
    @Operation(summary = "查看经验来源配置")
    @PreAuthorize("@permission.hasPermission('sys:experience:config')")
    public Result<List<ExperienceSourceConfigVO>> listSourceConfigs() {
        return Result.success(experienceAdminService.listSourceConfigs());
    }

    @PutMapping("/config")
    @Operation(summary = "更新经验来源配置")
    @PreAuthorize("@permission.hasPermission('sys:experience:config')")
    public Result<Void> updateSourceConfig(@RequestBody Map<String, String> body) {
        String configKey = body.get("configKey");
        String configValue = body.get("configValue");
        experienceAdminService.updateSourceConfig(configKey, configValue);
        return Result.success();
    }
}
