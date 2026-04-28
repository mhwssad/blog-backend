package com.cybzacg.blogbackend.module.auth.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.AuthenticationToken;
import com.cybzacg.blogbackend.module.auth.service.AccountTakeoverService;
import com.cybzacg.blogbackend.module.auth.token.TokenManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 账号接管认证控制器。
 *
 * <p>负责接收接管令牌并将操作者认证为目标用户。
 */
@RestController
@RequestMapping("/api/auth/takeover")
@Tag(name = "账号接管认证")
@RequiredArgsConstructor
public class TakeoverAuthController {
    private final AccountTakeoverService accountTakeoverService;
    private final TokenManager tokenManager;

    @PostMapping("/login")
    @Operation(summary = "使用接管令牌登录")
    public Result<AuthenticationToken> takeoverLogin(@Valid @RequestBody TakeoverLoginRequest body) {
        String takeoverToken = body.getTakeoverToken();
        Authentication authentication = accountTakeoverService.resolveTakeover(takeoverToken);
        AuthenticationToken token = tokenManager.generateToken(authentication);
        return Result.success(token);
    }

    @Data
    static class TakeoverLoginRequest {
        @NotBlank
        @Schema(description = "接管令牌")
        private String takeoverToken;
    }
}
