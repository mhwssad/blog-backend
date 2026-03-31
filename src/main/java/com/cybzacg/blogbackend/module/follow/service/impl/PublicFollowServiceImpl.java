package com.cybzacg.blogbackend.module.follow.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.mapper.SysUserFollowMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.follow.convert.FollowModelMapper;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;
import com.cybzacg.blogbackend.module.follow.service.PublicFollowService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 公开关注关系服务实现。
 */
@Service
@RequiredArgsConstructor
public class PublicFollowServiceImpl implements PublicFollowService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final SysUserFollowMapper sysUserFollowMapper;
    private final SysUserService sysUserService;
    private final FollowModelMapper followModelMapper;

    @Override
    public PageResult<PublicFollowUserVO> pageUserFollows(Long userId, PublicFollowPageQuery query) {
        requireActiveUser(userId);
        long current = normalizeCurrent(query == null ? null : query.getCurrent());
        long size = normalizeSize(query == null ? null : query.getSize());
        long total = defaultLong(sysUserFollowMapper.countPublicFollowPage(userId));
        if (total == 0L) {
            return emptyPage(current, size);
        }
        long offset = (current - 1) * size;
        List<PublicFollowUserVO> records = sysUserFollowMapper.selectPublicFollowPage(userId, offset, size)
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

    @Override
    public PageResult<PublicFollowUserVO> pageUserFans(Long userId, PublicFollowPageQuery query) {
        requireActiveUser(userId);
        long current = normalizeCurrent(query == null ? null : query.getCurrent());
        long size = normalizeSize(query == null ? null : query.getSize());
        long total = defaultLong(sysUserFollowMapper.countPublicFanPage(userId));
        if (total == 0L) {
            return emptyPage(current, size);
        }
        long offset = (current - 1) * size;
        List<PublicFollowUserVO> records = sysUserFollowMapper.selectPublicFanPage(userId, offset, size)
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
        SysUser user = sysUserService.getById(userId);
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

    private long normalizeCurrent(Long current) {
        return current == null || current < 1L ? 1L : current;
    }

    private long normalizeSize(Long size) {
        long normalized = size == null || size < 1L ? DEFAULT_PAGE_SIZE : size;
        return Math.min(normalized, MAX_PAGE_SIZE);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
