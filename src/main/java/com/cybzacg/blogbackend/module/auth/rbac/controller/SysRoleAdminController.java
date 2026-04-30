package com.cybzacg.blogbackend.module.auth.rbac.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.admin.*;
import com.cybzacg.blogbackend.module.auth.rbac.service.SysRoleAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统角色后台管理控制器。
 *
 * <p>负责对外暴露系统角色后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/roles")
@Tag(name = "后台角色管理")
@RequiredArgsConstructor
public class SysRoleAdminController {
    private final SysRoleAdminService sysRoleAdminService;

    @GetMapping
    @Operation(summary = "分页查询角色")
    @PreAuthorize("@permission.hasPermission('sys:role:query')")
    public Result<PageResult<SysRoleAdminVO>> pageRoles(SysRolePageQuery query) {
        return Result.success(sysRoleAdminService.pageRoles(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询角色详情")
    @PreAuthorize("@permission.hasPermission('sys:role:query')")
    public Result<SysRoleAdminVO> getRole(@PathVariable Long id) {
        return Result.success(sysRoleAdminService.getRole(id));
    }

    @PostMapping
    @Operation(summary = "新增角色")
    @PreAuthorize("@permission.hasPermission('sys:role:create')")
    public Result<SysRoleAdminVO> createRole(@Valid @RequestBody SysRoleSaveRequest request) {
        return Result.success(sysRoleAdminService.createRole(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改角色")
    @PreAuthorize("@permission.hasPermission('sys:role:update')")
    public Result<SysRoleAdminVO> updateRole(@PathVariable Long id,
                                             @Valid @RequestBody SysRoleSaveRequest request) {
        return Result.success(sysRoleAdminService.updateRole(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改角色状态")
    @PreAuthorize("@permission.hasPermission('sys:role:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        sysRoleAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @PreAuthorize("@permission.hasPermission('sys:role:delete')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        sysRoleAdminService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/{id}/menus")
    @Operation(summary = "查询角色菜单")
    @PreAuthorize("@permission.hasPermission('sys:role:query')")
    public Result<List<Long>> listMenuIds(@PathVariable Long id) {
        return Result.success(sysRoleAdminService.listMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    @Operation(summary = "分配角色菜单")
    @PreAuthorize("@permission.hasPermission('sys:role:assign-menu')")
    public Result<Void> assignMenus(@PathVariable Long id,
                                    @Valid @RequestBody RoleMenuAssignRequest request) {
        sysRoleAdminService.assignMenus(id, request.getMenuIds());
        return Result.success();
    }
}
