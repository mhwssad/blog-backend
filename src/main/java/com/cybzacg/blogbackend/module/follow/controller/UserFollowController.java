package com.cybzacg.blogbackend.module.follow.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.follow.model.user.*;
import com.cybzacg.blogbackend.module.follow.service.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户关注关系控制器。
 *
 * <p>负责对外暴露关注、取关、粉丝列表、关注列表和互关判断等用户侧接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户关注关系")
@RequiredArgsConstructor
@Validated
public class UserFollowController {
    private final UserFollowService userFollowService;

    @PostMapping("/follows/{userId}")
    @Operation(summary = "关注用户")
    public Result<Void> followUser(@PathVariable @NotNull @Positive Long userId) {
        log.info("开始关注用户, targetUserId={}", userId);
        userFollowService.followUser(userId);
        return Result.success();
    }

    @DeleteMapping("/follows/{userId}")
    @Operation(summary = "取消关注用户")
    public Result<Void> unfollowUser(@PathVariable @NotNull @Positive Long userId) {
        log.info("开始取消关注用户, targetUserId={}", userId);
        userFollowService.unfollowUser(userId);
        return Result.success();
    }

    @GetMapping("/follows")
    @Operation(summary = "分页查询我的关注列表")
    public Result<PageResult<UserFollowUserVO>> pageMyFollows(UserFollowPageQuery query) {
        log.info("开始分页查询我的关注列表, query={}", query);
        return Result.success(userFollowService.pageMyFollows(query));
    }

    @GetMapping("/fans")
    @Operation(summary = "分页查询我的粉丝列表")
    public Result<PageResult<UserFollowUserVO>> pageMyFans(UserFanPageQuery query) {
        log.info("开始分页查询我的粉丝列表, query={}", query);
        return Result.success(userFollowService.pageMyFans(query));
    }

    @GetMapping("/follows/mutual")
    @Operation(summary = "查询与目标用户的互关状态")
    public Result<UserFollowMutualVO> getMutualFollowStatus(@RequestParam @NotNull @Positive Long targetUserId) {
        log.info("开始查询互关状态, targetUserId={}", targetUserId);
        return Result.success(userFollowService.getMutualFollowStatus(targetUserId));
    }

    @GetMapping("/follows/count")
    @Operation(summary = "查询我的关注数和粉丝数")
    public Result<UserFollowCountVO> getMyFollowCount() {
        log.info("开始查询我的关注数和粉丝数");
        return Result.success(userFollowService.getMyFollowCount());
    }

    @PutMapping("/follows/{userId}/special")
    @Operation(summary = "设置或取消特别关注")
    public Result<Void> updateSpecialFollow(@PathVariable @NotNull @Positive Long userId,
                                            @Valid @RequestBody UserFollowSpecialUpdateRequest request) {
        log.info("开始设置或取消特别关注, targetUserId={}, request={}", userId, request);
        userFollowService.updateSpecialFollow(userId, request);
        return Result.success();
    }

    @PutMapping("/follows/{userId}/remark")
    @Operation(summary = "更新关注备注")
    public Result<Void> updateRemark(@PathVariable @NotNull @Positive Long userId,
                                     @Valid @RequestBody UserFollowRemarkUpdateRequest request) {
        log.info("开始更新关注备注, targetUserId={}, request={}", userId, request);
        userFollowService.updateRemark(userId, request);
        return Result.success();
    }
}
