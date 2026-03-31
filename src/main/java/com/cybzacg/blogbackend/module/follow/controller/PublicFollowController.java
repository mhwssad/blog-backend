package com.cybzacg.blogbackend.module.follow.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;
import com.cybzacg.blogbackend.module.follow.service.PublicFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开关注关系控制器。
 */
@RestController
@RequestMapping("/api/users/{userId}")
@Tag(name = "公开关注关系")
@RequiredArgsConstructor
public class PublicFollowController {
    private final PublicFollowService publicFollowService;

    @GetMapping("/follows")
    @Operation(summary = "分页查询指定用户的关注列表")
    public Result<PageResult<PublicFollowUserVO>> pageUserFollows(@PathVariable Long userId, PublicFollowPageQuery query) {
        return Result.success(publicFollowService.pageUserFollows(userId, query));
    }

    @GetMapping("/fans")
    @Operation(summary = "分页查询指定用户的粉丝列表")
    public Result<PageResult<PublicFollowUserVO>> pageUserFans(@PathVariable Long userId, PublicFollowPageQuery query) {
        return Result.success(publicFollowService.pageUserFans(userId, query));
    }
}
