package com.cybzacg.blogbackend.module.follow.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.follow.model.user.*;
import com.cybzacg.blogbackend.module.follow.service.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户关注关系控制器。
 *
 * <p>负责对外暴露关注、取关、粉丝列表、关注列表和互关判断等用户侧接口。
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户关注关系")
@RequiredArgsConstructor
public class UserFollowController {
    private final UserFollowService userFollowService;

    @PostMapping("/follows/{userId}")
    @Operation(summary = "关注用户")
    public Result<Void> followUser(@PathVariable Long userId) {
        userFollowService.followUser(userId);
        return Result.success();
    }

    @DeleteMapping("/follows/{userId}")
    @Operation(summary = "取消关注用户")
    public Result<Void> unfollowUser(@PathVariable Long userId) {
        userFollowService.unfollowUser(userId);
        return Result.success();
    }

    @GetMapping("/follows")
    @Operation(summary = "分页查询我的关注列表")
    public Result<PageResult<UserFollowUserVO>> pageMyFollows(UserFollowPageQuery query) {
        return Result.success(userFollowService.pageMyFollows(query));
    }

    @GetMapping("/fans")
    @Operation(summary = "分页查询我的粉丝列表")
    public Result<PageResult<UserFollowUserVO>> pageMyFans(UserFanPageQuery query) {
        return Result.success(userFollowService.pageMyFans(query));
    }

    @GetMapping("/follows/mutual")
    @Operation(summary = "查询与目标用户的互关状态")
    public Result<UserFollowMutualVO> getMutualFollowStatus(@RequestParam Long targetUserId) {
        return Result.success(userFollowService.getMutualFollowStatus(targetUserId));
    }

    @GetMapping("/follows/count")
    @Operation(summary = "查询我的关注数和粉丝数")
    public Result<UserFollowCountVO> getMyFollowCount() {
        return Result.success(userFollowService.getMyFollowCount());
    }

    @PutMapping("/follows/{userId}/special")
    @Operation(summary = "设置或取消特别关注")
    public Result<Void> updateSpecialFollow(@PathVariable Long userId,
                                            @Valid @RequestBody UserFollowSpecialUpdateRequest request) {
        userFollowService.updateSpecialFollow(userId, request);
        return Result.success();
    }

    @PutMapping("/follows/{userId}/remark")
    @Operation(summary = "更新关注备注")
    public Result<Void> updateRemark(@PathVariable Long userId,
                                     @Valid @RequestBody UserFollowRemarkUpdateRequest request) {
        userFollowService.updateRemark(userId, request);
        return Result.success();
    }
}
