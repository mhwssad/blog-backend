package com.cybzacg.blogbackend.module.follow.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.follow.convert.FollowModelMapper;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;
import com.cybzacg.blogbackend.module.follow.repository.SysUserFollowRepository;
import com.cybzacg.blogbackend.module.follow.service.PublicFollowService;
import com.cybzacg.blogbackend.utils.CollectionUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 公开关注关系服务实现。
 */
@Service
@RequiredArgsConstructor
public class PublicFollowServiceImpl implements PublicFollowService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final SysUserFollowRepository sysUserFollowRepository;
    private final SysUserRepository sysUserRepository;
    private final FollowModelMapper followModelMapper;

    /**
     * 分页查询指定用户的关注列表（公开接口）。
     */
    @Override
    public PageResult<PublicFollowUserVO> pageUserFollows(Long userId, PublicFollowPageQuery query) {
        requireActiveUser(userId);
        long current = PaginationUtils.normalizeCurrent(query == null ? null : query.getCurrent());
        long size = PaginationUtils.normalizeSize(query == null ? null : query.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        long total = CollectionUtils.defaultLong(sysUserFollowRepository.countPublicFollowPage(userId));
        if (total == 0L) {
            return emptyPage(current, size);
        }
        long offset = (current - 1) * size;
        List<PublicFollowUserVO> records = sysUserFollowRepository.selectPublicFollowPage(userId, offset, size)
                .stream()
                .map(followModelMapper::toPublicFollowUserVO)
                .toList();
        return PageResult.<PublicFollowUserVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    /**
     * 分页查询指定用户的粉丝列表（公开接口）。
     */
    @Override
    public PageResult<PublicFollowUserVO> pageUserFans(Long userId, PublicFollowPageQuery query) {
        requireActiveUser(userId);
        long current = PaginationUtils.normalizeCurrent(query == null ? null : query.getCurrent());
        long size = PaginationUtils.normalizeSize(query == null ? null : query.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        long total = CollectionUtils.defaultLong(sysUserFollowRepository.countPublicFanPage(userId));
        if (total == 0L) {
            return emptyPage(current, size);
        }
        long offset = (current - 1) * size;
        List<PublicFollowUserVO> records = sysUserFollowRepository.selectPublicFanPage(userId, offset, size)
                .stream()
                .map(followModelMapper::toPublicFollowUserVO)
                .toList();
        return PageResult.<PublicFollowUserVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    private void requireActiveUser(Long userId) {
        ExceptionThrowerCore.throwBusinessIfNull(userId, ResultErrorCode.USER_NOT_FOUND, "用户不存在");
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(
                user == null || !Objects.equals(user.getDeletedFlag(), 0) || !Objects.equals(user.getStatus(), 1),
                ResultErrorCode.USER_NOT_FOUND,
                "用户不存在"
        );
    }

    private PageResult<PublicFollowUserVO> emptyPage(long current, long size) {
        return PageResult.<PublicFollowUserVO>builder()
                .total(0L)
                .current(current)
                .size(size)
                .records(List.of())
                .build();
    }

}
