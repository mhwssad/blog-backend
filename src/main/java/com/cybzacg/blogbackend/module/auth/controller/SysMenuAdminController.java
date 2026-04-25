package com.cybzacg.blogbackend.module.auth.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysMenuAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统菜单后台管理控制器。
 *
 * <p>负责对外暴露系统菜单后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/menus")
@Tag(name = "后台菜单管理")
@RequiredArgsConstructor
public class SysMenuAdminController {
    private final SysMenuAdminService sysMenuAdminService;

    @GetMapping("/tree")
    @Operation(summary = "查询菜单树")
    @PreAuthorize("@permission.hasPermission('sys:menu:query')")
    public Result<List<SysMenuAdminVO>> listMenuTree() {
        return Result.success(sysMenuAdminService.listMenuTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询菜单详情")
    @PreAuthorize("@permission.hasPermission('sys:menu:query')")
    public Result<SysMenuAdminVO> getMenu(@PathVariable Long id) {
        return Result.success(sysMenuAdminService.getMenu(id));
    }

    @PostMapping
    @Operation(summary = "新增菜单")
    @PreAuthorize("@permission.hasPermission('sys:menu:create')")
    public Result<SysMenuAdminVO> createMenu(@Valid @RequestBody SysMenuSaveRequest request) {
        return Result.success(sysMenuAdminService.createMenu(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改菜单")
    @PreAuthorize("@permission.hasPermission('sys:menu:update')")
    public Result<SysMenuAdminVO> updateMenu(@PathVariable Long id,
                                             @Valid @RequestBody SysMenuSaveRequest request) {
        return Result.success(sysMenuAdminService.updateMenu(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    @PreAuthorize("@permission.hasPermission('sys:menu:delete')")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        sysMenuAdminService.deleteMenu(id);
        return Result.success();
    }
}
