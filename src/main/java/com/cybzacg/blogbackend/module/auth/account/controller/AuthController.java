package com.cybzacg.blogbackend.module.auth.account.controller;

import com.cybzacg.blogbackend.common.constant.HttpHeaderConstants;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.account.model.*;
import com.cybzacg.blogbackend.module.auth.account.service.AuthService;
import com.cybzacg.blogbackend.utils.RequestContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器。
 *
 * <p>负责对外暴露认证相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "账号登录")
    public Result<AuthenticationToken> login(@Valid @RequestBody AuthLoginRequest request) {
        return Result.success(authService.login(request, RequestContextUtils.getClientIp()));
    }

    @PostMapping("/register")
    @Operation(summary = "账号注册")
    public Result<AuthenticationToken> register(@Valid @RequestBody AuthRegisterRequest request) {
        return Result.success(authService.register(request, RequestContextUtils.getClientIp()));
    }

    @PostMapping("/email-code")
    @Operation(summary = "发送邮箱登录验证码")
    public Result<Void> sendEmailCode(@Valid @RequestBody AuthEmailCodeRequest request) {
        authService.sendEmailLoginCode(request);
        return Result.success();
    }

    @PostMapping("/email-login")
    @Operation(summary = "邮箱验证码登录")
    public Result<AuthenticationToken> emailLogin(@Valid @RequestBody AuthEmailLoginRequest request) {
        return Result.success(authService.emailLogin(request, RequestContextUtils.getClientIp()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public Result<AuthenticationToken> refresh(@Valid @RequestBody AuthRefreshRequest request) {
        return Result.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout(@RequestBody(required = false) LogoutRequest request,
                               @RequestHeader(value = HttpHeaderConstants.AUTHORIZATION, required = false) String authorization) {
        String token = request != null && StringUtils.hasText(request.getAccessToken())
                ? request.getAccessToken()
                : authorization;
        authService.logout(token);
        return Result.success();
    }

    @GetMapping("/current-user")
    @Operation(summary = "获取当前登录用户")
    public Result<AuthUserInfo> currentUser() {
        return Result.success(authService.getCurrentUser());
    }

    @GetMapping("/current-user-menus")
    @Operation(summary = "获取当前用户菜单")
    public Result<List<AuthMenuInfo>> currentUserMenus() {
        return Result.success(authService.getCurrentUserMenus());
    }
}
