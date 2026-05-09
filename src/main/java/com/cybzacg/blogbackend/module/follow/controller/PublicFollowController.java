package com.cybzacg.blogbackend.module.follow.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;
import com.cybzacg.blogbackend.module.follow.service.PublicFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开关注关系控制器。
 *
 * <p>负责查询指定用户的关注列表与粉丝列表等无需登录的公开接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/users/{userId}")
@Tag(name = "公开关注关系")
@RequiredArgsConstructor
@Validated
public class PublicFollowController {
    private final PublicFollowService publicFollowService;

    @GetMapping("/follows")
    @Operation(summary = "分页查询指定用户的关注列表")
    public Result<PageResult<PublicFollowUserVO>> pageUserFollows(@PathVariable @NotNull @Positive Long userId, PublicFollowPageQuery query) {
        log.info("开始分页查询用户关注列表, userId={}, query={}", userId, query);
        return Result.success(publicFollowService.pageUserFollows(userId, query));
    }

    @GetMapping("/fans")
    @Operation(summary = "分页查询指定用户的粉丝列表")
    public Result<PageResult<PublicFollowUserVO>> pageUserFans(@PathVariable @NotNull @Positive Long userId, PublicFollowPageQuery query) {
        log.info("开始分页查询用户粉丝列表, userId={}, query={}", userId, query);
        return Result.success(publicFollowService.pageUserFans(userId, query));
    }
}
