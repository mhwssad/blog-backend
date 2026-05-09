package com.cybzacg.blogbackend.module.follow.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.follow.SysUserFollow;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.follow.convert.FollowModelConvert;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import com.cybzacg.blogbackend.module.follow.model.user.*;
import com.cybzacg.blogbackend.module.follow.repository.SysUserFollowRepository;
import com.cybzacg.blogbackend.module.follow.service.FollowNoticeService;
import com.cybzacg.blogbackend.module.follow.service.UserFollowService;
import com.cybzacg.blogbackend.utils.CollectionUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 用户关注关系服务实现。
 *
 * <p>负责统一收口关注、取关、软取消恢复、列表查询和关注资料维护等用户侧主链路。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserFollowServiceImpl implements UserFollowService {
    private static final int FOLLOW_STATUS_ACTIVE = 1;
    private static final int FOLLOW_STATUS_INACTIVE = 0;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;
    private static final Duration FOLLOW_COUNT_CACHE_TTL = Duration.ofMinutes(5);

    private final SysUserFollowRepository sysUserFollowRepository;
    private final SysUserRepository sysUserRepository;
    private final FollowModelConvert followModelConvert;
    private final FollowNoticeService followNoticeService;
    private final RedisOperator redisOperator;

    /**
     * 创建或恢复一条单向关注关系；若已关注则保持幂等。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long targetUserId) {
        Long userId = SecurityUtils.requireUserId();
        requireActiveTargetUser(userId, targetUserId);
        SysUserFollow relation = getFollowRelation(userId, targetUserId);
        LocalDateTime now = LocalDateTime.now();
        if (relation != null) {
            if (isActiveRelation(relation)) {
                return;
            }
            reactivateFollowRelation(relation, now);
            evictFollowCountCache(userId, targetUserId);
            followNoticeService.notifyNewFollowerAfterCommit(targetUserId, userId);
            return;
        }
        if (createFollowRelation(userId, targetUserId, now)) {
            evictFollowCountCache(userId, targetUserId);
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
        relation.setUnfollowTime(LocalDateTime.now());
        sysUserFollowRepository.updateById(relation);
        evictFollowCountCache(userId, targetUserId);
    }

    /**
     * 分页查询当前用户的关注列表，支持按特别关注筛选。
     */
    @Override
    public PageResult<UserFollowUserVO> pageMyFollows(UserFollowPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        long current = PaginationUtils.normalizeCurrent(query == null ? null : query.getCurrent());
        long size = PaginationUtils.normalizeSize(query == null ? null : query.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        Boolean specialOnly = query == null ? null : query.getSpecialOnly();
        long total = CollectionUtils.defaultLong(sysUserFollowRepository.countFollowPage(userId, specialOnly));
        if (total == 0L) {
            return emptyPageResult(current, size);
        }
        long offset = (current - 1) * size;
        List<UserFollowUserVO> records = mapUserRecords(
                sysUserFollowRepository.selectFollowPage(userId, specialOnly, offset, size)
        );
        return PageResult.of(total, current, size, records);
    }

    /**
     * 分页查询当前用户的粉丝列表。
     */
    @Override
    public PageResult<UserFollowUserVO> pageMyFans(UserFanPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        long current = PaginationUtils.normalizeCurrent(query == null ? null : query.getCurrent());
        long size = PaginationUtils.normalizeSize(query == null ? null : query.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        long total = CollectionUtils.defaultLong(sysUserFollowRepository.countFanPage(userId));
        if (total == 0L) {
            return emptyPageResult(current, size);
        }
        long offset = (current - 1) * size;
        List<UserFollowUserVO> records = mapUserRecords(sysUserFollowRepository.selectFanPage(userId, offset, size));
        return PageResult.of(total, current, size, records);
    }

    /**
     * 查询当前用户与目标用户的互关状态。
     *
     * @param targetUserId 目标用户ID
     * @return 包含是否关注、是否被关注及是否互关的状态视图
     */
    @Override
    public UserFollowMutualVO getMutualFollowStatus(Long targetUserId) {
        Long userId = SecurityUtils.requireUserId();
        requireActiveTargetUser(userId, targetUserId);
        boolean following = CollectionUtils.defaultLong(sysUserFollowRepository.countActiveRelation(userId, targetUserId)) > 0L;
        boolean followedBy = CollectionUtils.defaultLong(sysUserFollowRepository.countActiveRelation(targetUserId, userId)) > 0L;
        return UserFollowMutualVO.builder()
                .targetUserId(targetUserId)
                .following(following)
                .followedBy(followedBy)
                .mutualFollow(following && followedBy)
                .build();
    }

    /**
     * 查询当前用户的关注数与粉丝数。
     */
    @Override
    public UserFollowCountVO getMyFollowCount() {
        Long userId = SecurityUtils.requireUserId();
        String cacheKey = followCountCacheKey(userId);
        UserFollowCountVO cached = readFollowCountCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        UserFollowCountVO result = UserFollowCountVO.builder()
                .followingCount(CollectionUtils.defaultLong(sysUserFollowRepository.countActiveFollowing(userId)))
                .fanCount(CollectionUtils.defaultLong(sysUserFollowRepository.countActiveFans(userId)))
                .build();
        writeFollowCountCache(cacheKey, result);
        return result;
    }

    /**
     * 更新我对目标用户的特别关注状态，仅允许操作有效关注关系。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialFollow(Long targetUserId, UserFollowSpecialUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        SysUserFollow relation = requireActiveFollowRelation(userId, targetUserId);
        followModelConvert.updateSpecialFollow(request, relation);
        sysUserFollowRepository.updateById(relation);
    }

    /**
     * 更新我对目标用户的备注；空白备注会被归一化为清空。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRemark(Long targetUserId, UserFollowRemarkUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        SysUserFollow relation = requireActiveFollowRelation(userId, targetUserId);
        followModelConvert.updateRemark(request, relation);
        sysUserFollowRepository.updateById(relation);
    }

    /**
     * 创建新关注关系，并在并发插入撞上唯一键时回退到“读取后恢复”的收口路径。
     * @return true 表示成功创建或恢复；false 表示已是有效关注关系无需处理
     */
    private boolean createFollowRelation(Long userId, Long targetUserId, LocalDateTime now) {
        SysUserFollow created = followModelConvert.toNewFollow(userId, targetUserId, now);
        try {
            sysUserFollowRepository.save(created);
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
    private void reactivateFollowRelation(SysUserFollow relation, LocalDateTime now) {
        relation.setFollowStatus(FOLLOW_STATUS_ACTIVE);
        relation.setFollowTime(now);
        relation.setUnfollowTime(null);
        relation.setSource("manual");
        sysUserFollowRepository.updateById(relation);
    }

    /**
     * 查询活跃的关注关系，若不存在或已取关则抛出业务异常。
     */
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

    /**
     * 查询关注关系，若不存在则返回 null。
     */
    private SysUserFollow getFollowRelation(Long userId, Long targetUserId) {
        return sysUserFollowRepository.findByFollowerAndFollowing(userId, targetUserId);
    }

    /**
     * 校验目标用户是否存在、可用且不是自己，避免非法关注关系写入。
     */
    private void requireActiveTargetUser(Long userId, Long targetUserId) {
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, targetUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能关注自己");
        SysUser targetUser = sysUserRepository.getById(targetUserId);
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

    /**
     * 判断关注关系是否处于活跃状态。
     */
    private boolean isActiveRelation(SysUserFollow relation) {
        return Objects.equals(relation.getFollowStatus(), FOLLOW_STATUS_ACTIVE);
    }

    /**
     * 将数据库记录映射为 VO 对象列表。
     */
    private List<UserFollowUserVO> mapUserRecords(List<FollowRelationUserItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().map(followModelConvert::toUserFollowUserVO).toList();
    }

    /**
     * 构建空的分页结果。
     */
    private PageResult<UserFollowUserVO> emptyPageResult(long current, long size) {
        return PageResult.empty(current, size);
    }

    /**
     * 从 Redis 读取关注计数缓存，读取失败时返回 null 并触发日志警告。
     */
    private UserFollowCountVO readFollowCountCache(String cacheKey) {
        try {
            return redisOperator.get(cacheKey, UserFollowCountVO.class);
        } catch (RuntimeException ex) {
            log.warn("读取关注计数缓存失败，回源数据库: key={}", cacheKey, ex);
            return null;
        }
    }

    /**
     * 将关注计数结果写入 Redis 缓存，写入失败时仅记录日志不阻断业务。
     */
    private void writeFollowCountCache(String cacheKey, UserFollowCountVO result) {
        try {
            redisOperator.set(cacheKey, result, FOLLOW_COUNT_CACHE_TTL);
        } catch (RuntimeException ex) {
            log.warn("写入关注计数缓存失败，不影响查询: key={}", cacheKey, ex);
        }
    }

    /**
     * 清除关注计数缓存，同时清理关注者和被关注者两侧的缓存。
     */
    private void evictFollowCountCache(Long followerId, Long followingId) {
        evictFollowCountCache(followerId);
        evictFollowCountCache(followingId);
    }

    /**
     * 清除指定用户的关注计数缓存，缓存清除失败时记录日志但不阻断业务。
     */
    private void evictFollowCountCache(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisOperator.delete(followCountCacheKey(userId));
        } catch (RuntimeException ex) {
            log.warn("清理关注计数缓存失败，不影响关系变更: userId={}", userId, ex);
        }
    }

    /**
     * 构建用户关注计数的 Redis 缓存 Key。
     */
    private String followCountCacheKey(Long userId) {
        return RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, userId);
    }

}
