package com.cybzacg.blogbackend.module.follow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.SysUserFollow;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.mapper.SysUserFollowMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.follow.convert.FollowModelMapper;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import com.cybzacg.blogbackend.module.follow.model.user.UserFanPageQuery;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowCountVO;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowMutualVO;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowRemarkUpdateRequest;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowSpecialUpdateRequest;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowUserVO;
import com.cybzacg.blogbackend.module.follow.service.FollowNoticeService;
import com.cybzacg.blogbackend.module.follow.service.UserFollowService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户关注关系服务实现。
 *
 * <p>负责统一收口关注、取关、软取消恢复、列表查询和关注资料维护等用户侧主链路。
 */
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {
    private static final int FOLLOW_STATUS_ACTIVE = 1;
    private static final int FOLLOW_STATUS_INACTIVE = 0;
    private static final int SPECIAL_FOLLOW_ENABLED = 1;
    private static final int SPECIAL_FOLLOW_DISABLED = 0;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final SysUserFollowMapper sysUserFollowMapper;
    private final SysUserService sysUserService;
    private final FollowModelMapper followModelMapper;
    private final FollowNoticeService followNoticeService;

    /**
     * 创建或恢复一条单向关注关系；若已关注则保持幂等。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long targetUserId) {
        Long userId = SecurityUtils.requireUserId();
        requireActiveTargetUser(userId, targetUserId);
        SysUserFollow relation = getFollowRelation(userId, targetUserId);
        Date now = new Date();
        if (relation != null) {
            if (isActiveRelation(relation)) {
                return;
            }
            reactivateFollowRelation(relation, now);
            followNoticeService.notifyNewFollowerAfterCommit(targetUserId, userId);
            return;
        }
        if (createFollowRelation(userId, targetUserId, now)) {
            followNoticeService.notifyNewFollowerAfterCommit(targetUserId, userId);
        }
    }

    /**
     * 将已存在的关注关系标记为取关；重复取关时直接忽略。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long targetUserId) {
        Long userId = SecurityUtils.requireUserId();
        SysUserFollow relation = requireActiveFollowRelation(userId, targetUserId);
        relation.setFollowStatus(FOLLOW_STATUS_INACTIVE);
        relation.setUnfollowTime(new Date());
        sysUserFollowMapper.updateById(relation);
    }

    @Override
    public PageResult<UserFollowUserVO> pageMyFollows(UserFollowPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        long current = normalizeCurrent(query == null ? null : query.getCurrent());
        long size = normalizeSize(query == null ? null : query.getSize());
        Boolean specialOnly = query == null ? null : query.getSpecialOnly();
        long total = defaultLong(sysUserFollowMapper.countFollowPage(userId, specialOnly));
        if (total == 0L) {
            return emptyPageResult(current, size);
        }
        long offset = (current - 1) * size;
        List<UserFollowUserVO> records = mapUserRecords(
                sysUserFollowMapper.selectFollowPage(userId, specialOnly, offset, size)
        );
        return PageResult.<UserFollowUserVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    @Override
    public PageResult<UserFollowUserVO> pageMyFans(UserFanPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        long current = normalizeCurrent(query == null ? null : query.getCurrent());
        long size = normalizeSize(query == null ? null : query.getSize());
        long total = defaultLong(sysUserFollowMapper.countFanPage(userId));
        if (total == 0L) {
            return emptyPageResult(current, size);
        }
        long offset = (current - 1) * size;
        List<UserFollowUserVO> records = mapUserRecords(sysUserFollowMapper.selectFanPage(userId, offset, size));
        return PageResult.<UserFollowUserVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    @Override
    public UserFollowMutualVO getMutualFollowStatus(Long targetUserId) {
        Long userId = SecurityUtils.requireUserId();
        requireActiveTargetUser(userId, targetUserId);
        boolean following = defaultLong(sysUserFollowMapper.countActiveRelation(userId, targetUserId)) > 0L;
        boolean followedBy = defaultLong(sysUserFollowMapper.countActiveRelation(targetUserId, userId)) > 0L;
        return UserFollowMutualVO.builder()
                .targetUserId(targetUserId)
                .following(following)
                .followedBy(followedBy)
                .mutualFollow(following && followedBy)
                .build();
    }

    @Override
    public UserFollowCountVO getMyFollowCount() {
        Long userId = SecurityUtils.requireUserId();
        return UserFollowCountVO.builder()
                .followingCount(defaultLong(sysUserFollowMapper.countActiveFollowing(userId)))
                .fanCount(defaultLong(sysUserFollowMapper.countActiveFans(userId)))
                .build();
    }

    /**
     * 更新我对目标用户的特别关注状态，仅允许操作有效关注关系。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialFollow(Long targetUserId, UserFollowSpecialUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        validateSpecialFollowValue(request);
        SysUserFollow relation = requireActiveFollowRelation(userId, targetUserId);
        followModelMapper.updateSpecialFollow(request, relation);
        sysUserFollowMapper.updateById(relation);
    }

    /**
     * 更新我对目标用户的备注；空白备注会被归一化为清空。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRemark(Long targetUserId, UserFollowRemarkUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        SysUserFollow relation = requireActiveFollowRelation(userId, targetUserId);
        followModelMapper.updateRemark(request, relation);
        sysUserFollowMapper.updateById(relation);
    }

    /**
     * 创建新关注关系，并在并发插入撞上唯一键时回退到“读取后恢复”的收口路径。
     */
    private boolean createFollowRelation(Long userId, Long targetUserId, Date now) {
        SysUserFollow created = followModelMapper.toNewFollow(userId, targetUserId, now);
        try {
            sysUserFollowMapper.insert(created);
            return true;
        } catch (DuplicateKeyException ex) {
            SysUserFollow existing = getFollowRelation(userId, targetUserId);
            if (existing == null) {
                throw ex;
            }
            if (!isActiveRelation(existing)) {
                reactivateFollowRelation(existing, now);
                return true;
            }
            return false;
        }
    }

    /**
     * 复用旧记录恢复关注，保持“软取消后重新关注”不新增关系行的设计。
     */
    private void reactivateFollowRelation(SysUserFollow relation, Date now) {
        relation.setFollowStatus(FOLLOW_STATUS_ACTIVE);
        relation.setFollowTime(now);
        relation.setUnfollowTime(null);
        relation.setSource("manual");
        sysUserFollowMapper.updateById(relation);
    }

    private SysUserFollow requireActiveFollowRelation(Long userId, Long targetUserId) {
        requireActiveTargetUser(userId, targetUserId);
        SysUserFollow relation = getFollowRelation(userId, targetUserId);
        ExceptionThrowerCore.throwBusinessIf(
                relation == null || !isActiveRelation(relation),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "关注关系不存在"
        );
        return relation;
    }

    private SysUserFollow getFollowRelation(Long userId, Long targetUserId) {
        return sysUserFollowMapper.selectOne(Wrappers.lambdaQuery(SysUserFollow.class)
                .eq(SysUserFollow::getFollowerId, userId)
                .eq(SysUserFollow::getFollowingId, targetUserId)
                .last("limit 1"));
    }

    /**
     * 校验目标用户是否存在、可用且不是自己，避免非法关注关系写入。
     */
    private void requireActiveTargetUser(Long userId, Long targetUserId) {
        ExceptionThrowerCore.throwBusinessIfNull(targetUserId, ResultErrorCode.USER_NOT_FOUND, "用户不存在");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, targetUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能关注自己");
        SysUser targetUser = sysUserService.getById(targetUserId);
        ExceptionThrowerCore.throwBusinessIf(
                targetUser == null || !Objects.equals(targetUser.getDeletedFlag(), 0),
                ResultErrorCode.USER_NOT_FOUND,
                "用户不存在"
        );
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(targetUser.getStatus(), 1),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "目标用户不可用"
        );
    }

    private void validateSpecialFollowValue(UserFollowSpecialUpdateRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "更新参数不能为空");
        Integer specialFollow = request.getSpecialFollow();
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(specialFollow, SPECIAL_FOLLOW_DISABLED)
                        && !Objects.equals(specialFollow, SPECIAL_FOLLOW_ENABLED),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "特别关注状态不合法"
        );
    }

    private boolean isActiveRelation(SysUserFollow relation) {
        return Objects.equals(relation.getFollowStatus(), FOLLOW_STATUS_ACTIVE);
    }

    private List<UserFollowUserVO> mapUserRecords(List<FollowRelationUserItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().map(followModelMapper::toUserFollowUserVO).toList();
    }

    private PageResult<UserFollowUserVO> emptyPageResult(long current, long size) {
        return PageResult.<UserFollowUserVO>builder()
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
