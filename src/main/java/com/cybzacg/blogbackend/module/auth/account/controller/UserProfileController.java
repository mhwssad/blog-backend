package com.cybzacg.blogbackend.module.auth.account.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserPasswordChangeRequest;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileUpdateRequest;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileVO;
import com.cybzacg.blogbackend.module.auth.account.service.UserProfileService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户自服务控制器。
 */
@RestController
@RequestMapping("/api/user/profile")
@Tag(name = "用户自服务")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "查询个人资料")
    public Result<UserProfileVO> getProfile() {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(userProfileService.getProfile(userId));
    }

    @PutMapping
    @Operation(summary = "更新公开资料")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(userProfileService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@Valid @RequestBody UserPasswordChangeRequest request) {
        Long userId = SecurityUtils.requireUserId();
        userProfileService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }
}
