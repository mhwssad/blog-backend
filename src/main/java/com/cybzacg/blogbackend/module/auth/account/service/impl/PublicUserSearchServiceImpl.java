package com.cybzacg.blogbackend.module.auth.account.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.convert.UserProfileModelConvert;
import com.cybzacg.blogbackend.module.auth.account.model.user.PublicUserSearchVO;
import com.cybzacg.blogbackend.module.auth.account.service.PublicUserSearchService;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公开用户搜索服务实现。
 */
@Service
@RequiredArgsConstructor
public class PublicUserSearchServiceImpl implements PublicUserSearchService {

    private final SysUserRepository sysUserRepository;
    private final UserProfileModelConvert userProfileModelConvert;

    @Override
    public PageResult<PublicUserSearchVO> searchUsers(String keyword, long current, long size) {
        current = PaginationUtils.normalizeCurrent(current);
        size = PaginationUtils.normalizeSize(size, 20L, 50L);
        Page<SysUser> result = sysUserRepository.searchByKeyword(StrUtils.trim(keyword), current, size);

        List<PublicUserSearchVO> voList = result.getRecords().stream()
                .map(userProfileModelConvert::toPublicUserSearchVO)
                .toList();
        return PageResult.of(result, voList);
    }
}
