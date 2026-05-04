package com.cybzacg.blogbackend.module.auth.account.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.account.model.user.PasswordResetCodeRequest;
import com.cybzacg.blogbackend.module.auth.account.model.user.PasswordResetRequest;
import com.cybzacg.blogbackend.module.auth.account.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 找回密码控制器。
 */
@RestController
@RequestMapping("/api/auth/password-reset")
@Tag(name = "找回密码")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/code")
    @Operation(summary = "发送找回密码验证码")
    public Result<Void> sendResetCode(@Valid @RequestBody PasswordResetCodeRequest request) {
        passwordResetService.sendResetCode(request.getEmail());
        return Result.success();
    }

    @PostMapping
    @Operation(summary = "重置密码")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return Result.success();
    }
}
