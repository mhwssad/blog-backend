package com.cybzacg.blogbackend.module.follow.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.follow.convert.FollowModelMapper;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminPageQuery;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminRelationVO;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowRelationCleanRequest;
import com.cybzacg.blogbackend.module.follow.repository.SysUserFollowRepository;
import com.cybzacg.blogbackend.module.follow.service.FollowAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关注关系后台管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class FollowAdminServiceImpl implements FollowAdminService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final SysUserFollowRepository sysUserFollowRepository;
    private final FollowModelMapper followModelMapper;

    @Override
    public PageResult<FollowAdminRelationVO> pageRelations(FollowAdminPageQuery query) {
        long current = normalizeCurrent(query == null ? null : query.getCurrent());
        long size = normalizeSize(query == null ? null : query.getSize());
        long total = defaultLong(sysUserFollowRepository.countAdminRelationPage(query));
        if (total == 0L) {
            return PageResult.<FollowAdminRelationVO>builder()
                    .total(0L)
                    .current(current)
                    .size(size)
                    .records(List.of())
                    .build();
        }
        long offset = (current - 1) * size;
        List<FollowAdminRelationVO> records = sysUserFollowRepository.selectAdminRelationPage(query, offset, size)
                .stream()
                .map(followModelMapper::toFollowAdminRelationVO)
                .toList();
        return PageResult.<FollowAdminRelationVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    /**
     * 清理已取关或用户状态异常导致的无效关系，避免后台长期积累脏数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long cleanRelations(FollowRelationCleanRequest request) {
        validateCleanRequest(request);
        long count = defaultLong(sysUserFollowRepository.countCleanableRelations(
                isTrue(request.getCleanInactive()),
                isTrue(request.getCleanDeletedUsers()),
                isTrue(request.getCleanDisabledUsers())
        ));
        if (count == 0L) {
            return 0L;
        }
        sysUserFollowRepository.deleteCleanableRelations(
                isTrue(request.getCleanInactive()),
                isTrue(request.getCleanDeletedUsers()),
                isTrue(request.getCleanDisabledUsers())
        );
        return count;
    }

    private void validateCleanRequest(FollowRelationCleanRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "清理条件不能为空");
        ExceptionThrowerCore.throwBusinessIf(
                !isTrue(request.getCleanInactive())
                        && !isTrue(request.getCleanDeletedUsers())
                        && !isTrue(request.getCleanDisabledUsers()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "清理关注关系必须至少指定一个条件"
        );
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
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
