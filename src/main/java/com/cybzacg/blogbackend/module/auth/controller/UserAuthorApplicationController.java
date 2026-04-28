package com.cybzacg.blogbackend.module.auth.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.user.UserAuthorApplicationPageQuery;
import com.cybzacg.blogbackend.module.auth.model.user.UserAuthorApplicationSubmitRequest;
import com.cybzacg.blogbackend.module.auth.model.user.UserAuthorApplicationVO;
import com.cybzacg.blogbackend.module.auth.service.UserAuthorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户作者申请控制器。
 */
@RestController
@RequestMapping("/api/user/author-applications")
@Tag(name = "用户作者申请")
@RequiredArgsConstructor
public class UserAuthorApplicationController {
    private final UserAuthorApplicationService userAuthorApplicationService;

    @PostMapping
    @Operation(summary = "提交作者申请")
    public Result<UserAuthorApplicationVO> submitApplication(@Valid @RequestBody UserAuthorApplicationSubmitRequest request) {
        return Result.success(userAuthorApplicationService.submitApplication(request));
    }

    @GetMapping("/latest")
    @Operation(summary = "查询最近一次作者申请")
    public Result<UserAuthorApplicationVO> getLatestApplication() {
        return Result.success(userAuthorApplicationService.getLatestApplication());
    }

    @GetMapping
    @Operation(summary = "分页查询我的作者申请记录")
    public Result<PageResult<UserAuthorApplicationVO>> pageMyApplications(UserAuthorApplicationPageQuery query) {
        return Result.success(userAuthorApplicationService.pageMyApplications(query));
    }
}
