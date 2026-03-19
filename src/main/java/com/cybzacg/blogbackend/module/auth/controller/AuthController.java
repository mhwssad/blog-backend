package com.cybzacg.blogbackend.module.auth.controller;

import com.cybzacg.blogbackend.common.constant.HttpHeaderConstants;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.AuthEmailCodeRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthEmailLoginRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthLoginRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthMenuInfo;
import com.cybzacg.blogbackend.module.auth.model.AuthRefreshRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthRegisterRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthUserInfo;
import com.cybzacg.blogbackend.module.auth.model.AuthenticationToken;
import com.cybzacg.blogbackend.module.auth.model.LogoutRequest;
import com.cybzacg.blogbackend.module.auth.service.AuthService;
import com.cybzacg.blogbackend.utils.IPUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<AuthenticationToken> login(@Valid @RequestBody AuthLoginRequest request,
                                             HttpServletRequest httpServletRequest) {
        return Result.success(authService.login(request, IPUtils.getIpAddr(httpServletRequest)));
    }

    @PostMapping("/register")
    @Operation(summary = "账号注册")
    public Result<AuthenticationToken> register(@Valid @RequestBody AuthRegisterRequest request,
                                                HttpServletRequest httpServletRequest) {
        return Result.success(authService.register(request, IPUtils.getIpAddr(httpServletRequest)));
    }

    @PostMapping("/email-code")
    @Operation(summary = "发送邮箱登录验证码")
    public Result<Void> sendEmailCode(@Valid @RequestBody AuthEmailCodeRequest request) {
        authService.sendEmailLoginCode(request);
        return Result.success();
    }

    @PostMapping("/email-login")
    @Operation(summary = "邮箱验证码登录")
    public Result<AuthenticationToken> emailLogin(@Valid @RequestBody AuthEmailLoginRequest request,
                                                  HttpServletRequest httpServletRequest) {
        return Result.success(authService.emailLogin(request, IPUtils.getIpAddr(httpServletRequest)));
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
