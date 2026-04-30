package com.cybzacg.blogbackend.module.auth.account.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.account.model.admin.PasswordResetRequest;
import com.cybzacg.blogbackend.module.auth.account.model.admin.StatusUpdateRequest;
import com.cybzacg.blogbackend.module.auth.account.service.SysUserAdminService;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserSaveRequest;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.UserRoleAssignRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统用户后台管理控制器。
 *
 * <p>负责对外暴露系统用户后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/users")
@Tag(name = "后台用户管理")
@RequiredArgsConstructor
public class SysUserAdminController {
    private final SysUserAdminService sysUserAdminService;

    @GetMapping
    @Operation(summary = "分页查询用户")
    @PreAuthorize("@permission.hasPermission('sys:user:query')")
    public Result<PageResult<SysUserAdminVO>> pageUsers(SysUserPageQuery query) {
        return Result.success(sysUserAdminService.pageUsers(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    @PreAuthorize("@permission.hasPermission('sys:user:query')")
    public Result<SysUserAdminVO> getUser(@PathVariable Long id) {
        return Result.success(sysUserAdminService.getUser(id));
    }

    @PostMapping
    @Operation(summary = "新增用户")
    @PreAuthorize("@permission.hasPermission('sys:user:create')")
    public Result<SysUserAdminVO> createUser(@Valid @RequestBody SysUserSaveRequest request) {
        return Result.success(sysUserAdminService.createUser(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改用户")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<SysUserAdminVO> updateUser(@PathVariable Long id,
                                             @Valid @RequestBody SysUserSaveRequest request) {
        return Result.success(sysUserAdminService.updateUser(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改用户状态")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        sysUserAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/password/reset")
    @Operation(summary = "重置用户密码")
    @PreAuthorize("@permission.hasPermission('sys:user:reset-password')")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody PasswordResetRequest request) {
        sysUserAdminService.resetPassword(id, request.getPassword());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("@permission.hasPermission('sys:user:delete')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        sysUserAdminService.deleteUser(id);
        return Result.success();
    }

    @GetMapping("/{id}/roles")
    @Operation(summary = "查询用户角色")
    @PreAuthorize("@permission.hasPermission('sys:user:query')")
    public Result<List<Long>> listRoleIds(@PathVariable Long id) {
        return Result.success(sysUserAdminService.listRoleIds(id));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "分配用户角色")
    @PreAuthorize("@permission.hasPermission('sys:user:assign-role')")
    public Result<Void> assignRoles(@PathVariable Long id,
                                    @Valid @RequestBody UserRoleAssignRequest request) {
        sysUserAdminService.assignRoles(id, request.getRoleIds());
        return Result.success();
    }
}
