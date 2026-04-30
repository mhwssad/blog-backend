package com.cybzacg.blogbackend.module.auth.account.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.account.model.admin.*;
import com.cybzacg.blogbackend.module.auth.account.service.AccountTakeoverService;
import com.cybzacg.blogbackend.module.auth.account.service.SysUserAdminService;
import com.cybzacg.blogbackend.module.auth.account.service.TwoFactorService;
import com.cybzacg.blogbackend.module.auth.config.model.admin.AdjustExperienceRequest;
import com.cybzacg.blogbackend.module.auth.config.model.admin.AdjustLevelRequest;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.UserRoleAuditAssignRequest;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * 超级管理员操作控制器。
 *
 * <p>负责对外暴露超级管理员专属操作接口，包括2FA二次验证、用户封禁/解封、
 * 等级与经验调整、账号接管、带审计的角色分配等。
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "超级管理员操作")
@RequiredArgsConstructor
public class SuperAdminController {
    private final TwoFactorService twoFactorService;
    private final SysUserAdminService sysUserAdminService;
    private final AccountTakeoverService accountTakeoverService;

    @PostMapping("/2fa/send-code")
    @Operation(summary = "发送2FA验证码")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<Void> sendMfaCode() {
        Long userId = SecurityUtils.requireUserId();
        twoFactorService.sendMfaCode(userId);
        return Result.success();
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "校验2FA验证码")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<MfaVerifyResponse> verifyMfaCode(@Valid @RequestBody MfaVerifyRequest request) {
        Long userId = SecurityUtils.requireUserId();
        String ticket = twoFactorService.verifyMfaCode(userId, request.getCode());
        long expiresIn = Duration.ofMinutes(30).getSeconds();
        return Result.success(new MfaVerifyResponse(ticket, expiresIn));
    }

    @PostMapping("/users/{id}/ban")
    @Operation(summary = "封禁用户")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<Void> banUser(@PathVariable Long id,
                                @Valid @RequestBody BanUserRequest request,
                                HttpServletRequest httpRequest) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        sysUserAdminService.banUser(operatorId, id, request.getMfaTicket(), ip, ua);
        return Result.success();
    }

    @PostMapping("/users/{id}/unban")
    @Operation(summary = "解封用户")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<Void> unbanUser(@PathVariable Long id,
                                  @Valid @RequestBody BanUserRequest request,
                                  HttpServletRequest httpRequest) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        sysUserAdminService.unbanUser(operatorId, id, request.getMfaTicket(), ip, ua);
        return Result.success();
    }

    @PutMapping("/users/{id}/level")
    @Operation(summary = "调整用户等级")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<Void> adjustLevel(@PathVariable Long id,
                                    @Valid @RequestBody AdjustLevelRequest request,
                                    HttpServletRequest httpRequest) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        sysUserAdminService.adjustLevel(operatorId, id, request.getLevel(), request.getMfaTicket(), ip, ua);
        return Result.success();
    }

    @PutMapping("/users/{id}/experience")
    @Operation(summary = "调整用户经验")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<Void> adjustExperience(@PathVariable Long id,
                                         @Valid @RequestBody AdjustExperienceRequest request,
                                         HttpServletRequest httpRequest) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        sysUserAdminService.adjustExperience(operatorId, id, request.getExperience(), request.getMfaTicket(), ip, ua);
        return Result.success();
    }

    @PostMapping("/takeover")
    @Operation(summary = "账号接管")
    @PreAuthorize("@permission.hasPermission('sys:user:update')")
    public Result<AccountTakeoverResponse> takeover(@Valid @RequestBody AccountTakeoverRequest request,
                                                     HttpServletRequest httpRequest) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return Result.success(accountTakeoverService.takeover(operatorId, request.getTargetUserId(),
                request.getMfaTicket(), ip, ua));
    }

    @PutMapping("/users/{id}/roles")
    @Operation(summary = "带审计的角色分配")
    @PreAuthorize("@permission.hasPermission('sys:user:assign-role')")
    public Result<Void> assignRolesWithAudit(@PathVariable Long id,
                                              @Valid @RequestBody UserRoleAuditAssignRequest request,
                                              HttpServletRequest httpRequest) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        sysUserAdminService.assignRolesWithAudit(operatorId, id, request.getRoleIds(),
                request.getMfaTicket(), ip, ua);
        return Result.success();
    }
}
