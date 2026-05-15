package com.cybzacg.blogbackend.module.auth.account.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.account.model.user.PublicUserSearchQuery;
import com.cybzacg.blogbackend.module.auth.account.model.user.PublicUserSearchVO;
import com.cybzacg.blogbackend.module.auth.account.service.PublicUserSearchService;
import com.cybzacg.blogbackend.utils.StrUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开用户搜索控制器。
 */
@RestController
@RequestMapping("/api/users/search")
@Tag(name = "公开用户搜索")
@RequiredArgsConstructor
public class PublicUserSearchController {

    private final PublicUserSearchService publicUserSearchService;

    @GetMapping
    @Operation(summary = "搜索用户")
    public Result<PageResult<PublicUserSearchVO>> searchUsers(PublicUserSearchQuery query) {
        String keyword = query.getKeyword();
        if (!StrUtils.hasText(keyword) || StrUtils.trim(keyword).length() < 2) {
            return Result.success(PageResult.empty());
        }
        return Result.success(publicUserSearchService.searchUsers(keyword, query.getCurrent(), query.getSize()));
    }
}
