package com.cybzacg.blogbackend.module.auth.account.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.module.auth.account.convert.UserProfileModelConvert;
import com.cybzacg.blogbackend.module.auth.account.model.user.PublicUserSearchQuery;
import com.cybzacg.blogbackend.module.auth.account.model.user.PublicUserSearchVO;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开用户搜索控制器。
 */
@RestController
@RequestMapping("/api/users/search")
@Tag(name = "公开用户搜索")
@RequiredArgsConstructor
public class PublicUserSearchController {

    private final SysUserRepository sysUserRepository;
    private final UserProfileModelConvert userProfileModelConvert;

    @GetMapping
    @Operation(summary = "搜索用户")
    public Result<PageResult<PublicUserSearchVO>> searchUsers(PublicUserSearchQuery query) {
        String keyword = query.getKeyword();
        if (!StringUtils.hasText(keyword) || keyword.trim().length() < 2) {
            return Result.success(PageResult.of(0L, List.of()));
        }

        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 50L);
        Page<SysUser> result = sysUserRepository.searchByKeyword(keyword.trim(), current, size);

        List<PublicUserSearchVO> voList = result.getRecords().stream()
                .map(userProfileModelConvert::toPublicUserSearchVO)
                .toList();
        return Result.success(PageResult.of(result, voList));
    }
}
