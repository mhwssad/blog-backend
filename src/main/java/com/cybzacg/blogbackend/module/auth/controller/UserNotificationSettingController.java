package com.cybzacg.blogbackend.module.auth.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingBatchUpdateRequest;
import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingItemVO;
import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingStatusUpdateRequest;
import com.cybzacg.blogbackend.module.auth.service.UserNotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户通知设置控制器。
 */
@RestController
@RequestMapping("/api/user/notification-settings")
@Tag(name = "用户通知设置")
@RequiredArgsConstructor
public class UserNotificationSettingController {
    private final UserNotificationSettingService userNotificationSettingService;

    @GetMapping
    @Operation(summary = "查询我的通知设置")
    public Result<List<UserNotificationSettingItemVO>> listMySettings() {
        return Result.success(userNotificationSettingService.listMySettings());
    }

    @PutMapping
    @Operation(summary = "批量更新我的通知设置")
    public Result<Void> updateMySettings(@Valid @RequestBody UserNotificationSettingBatchUpdateRequest request) {
        userNotificationSettingService.updateMySettings(request);
        return Result.success();
    }

    @PutMapping("/{type}")
    @Operation(summary = "单独更新某类通知设置")
    public Result<Void> updateMySetting(@PathVariable String type,
                                        @Valid @RequestBody UserNotificationSettingStatusUpdateRequest request) {
        userNotificationSettingService.updateMySetting(type, request);
        return Result.success();
    }
}
