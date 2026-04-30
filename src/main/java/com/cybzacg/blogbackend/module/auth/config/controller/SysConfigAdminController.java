package com.cybzacg.blogbackend.module.auth.config.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.config.model.admin.SysConfigAdminVO;
import com.cybzacg.blogbackend.module.auth.config.model.admin.SysConfigPageQuery;
import com.cybzacg.blogbackend.module.auth.config.model.admin.SysConfigSaveRequest;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置后台管理控制器。
 *
 * <p>负责对外暴露系统配置后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/configs")
@Tag(name = "系统配置管理")
@RequiredArgsConstructor
public class SysConfigAdminController {
    private final SysConfigAdminService sysConfigAdminService;

    @GetMapping
    @Operation(summary = "分页查询配置")
    @PreAuthorize("@permission.hasPermission('sys:config:query')")
    public Result<PageResult<SysConfigAdminVO>> pageConfigs(SysConfigPageQuery query) {
        return Result.success(sysConfigAdminService.pageConfigs(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询配置详情")
    @PreAuthorize("@permission.hasPermission('sys:config:query')")
    public Result<SysConfigAdminVO> getConfig(@PathVariable Long id) {
        return Result.success(sysConfigAdminService.getConfig(id));
    }

    @PostMapping
    @Operation(summary = "新增配置")
    @PreAuthorize("@permission.hasPermission('sys:config:create')")
    public Result<SysConfigAdminVO> createConfig(@Valid @RequestBody SysConfigSaveRequest request) {
        return Result.success(sysConfigAdminService.createConfig(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改配置")
    @PreAuthorize("@permission.hasPermission('sys:config:update')")
    public Result<SysConfigAdminVO> updateConfig(@PathVariable Long id,
                                                 @Valid @RequestBody SysConfigSaveRequest request) {
        return Result.success(sysConfigAdminService.updateConfig(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    @PreAuthorize("@permission.hasPermission('sys:config:delete')")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        sysConfigAdminService.deleteConfig(id);
        return Result.success();
    }

    @GetMapping("/key/{configKey}")
    @Operation(summary = "按配置键查询配置值")
    @PreAuthorize("@permission.hasPermission('sys:config:query')")
    public Result<String> getValueByKey(@PathVariable String configKey) {
        return Result.success(sysConfigAdminService.getValueByKey(configKey));
    }
}
