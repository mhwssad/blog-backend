package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.follow.SysUserFollow;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.follow.convert.FollowModelConvert;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import com.cybzacg.blogbackend.module.follow.model.user.*;
import com.cybzacg.blogbackend.module.follow.repository.SysUserFollowRepository;
import com.cybzacg.blogbackend.module.follow.service.FollowNoticeService;
import com.cybzacg.blogbackend.module.follow.service.impl.UserFollowServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFollowServiceImplTest {
    @Mock
    private SysUserFollowRepository sysUserFollowRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private FollowModelConvert followModelConvert;
    @Mock
    private FollowNoticeService followNoticeService;
    @Mock
    private RedisOperator redisOperator;

    private UserFollowServiceImpl userFollowService;

    @BeforeEach
    void setUp() {
        userFollowService = new UserFollowServiceImpl(
                sysUserFollowRepository,
                sysUserRepository,
                followModelConvert,
                followNoticeService,
                redisOperator
        );
    }

    @Test
    void followUserShouldInsertNewRelationWhenAbsent() {
        SysUser targetUser = activeUser(12L);
        SysUserFollow created = new SysUserFollow();
        created.setFollowerId(7L);
        created.setFollowingId(12L);
        created.setFollowStatus(1);

        when(sysUserRepository.getById(12L)).thenReturn(targetUser);
        when(sysUserFollowRepository.findByFollowerAndFollowing(7L, 12L)).thenReturn(null);
        when(followModelConvert.toNewFollow(eq(7L), eq(12L), any())).thenReturn(created);
        when(sysUserFollowRepository.save(created)).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userFollowService.followUser(12L);
        }

        verify(followModelConvert).toNewFollow(eq(7L), eq(12L), any());
        verify(sysUserFollowRepository).save(created);
        verify(redisOperator).delete(RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 7L));
        verify(redisOperator).delete(RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 12L));
        verify(sysUserFollowRepository, never()).updateById(any(SysUserFollow.class));
        verify(followNoticeService).notifyNewFollowerAfterCommit(12L, 7L);
    }

    @Test
    void followUserShouldReactivateExistingRelationWhenPreviouslyUnfollowed() {
        SysUser targetUser = activeUser(12L);
        SysUserFollow relation = new SysUserFollow();
        relation.setId(20L);
        relation.setFollowerId(7L);
        relation.setFollowingId(12L);
        relation.setFollowStatus(0);

        when(sysUserRepository.getById(12L)).thenReturn(targetUser);
        when(sysUserFollowRepository.findByFollowerAndFollowing(7L, 12L)).thenReturn(relation);
        when(sysUserFollowRepository.updateById(relation)).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userFollowService.followUser(12L);
        }

        assertEquals(Integer.valueOf(1), relation.getFollowStatus());
        assertEquals("manual", relation.getSource());
        verify(sysUserFollowRepository).updateById(relation);
        verify(redisOperator).delete(RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 7L));
        verify(redisOperator).delete(RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 12L));
        verify(sysUserFollowRepository, never()).save(any(SysUserFollow.class));
        verify(followNoticeService).notifyNewFollowerAfterCommit(12L, 7L);
    }

    @Test
    void followUserShouldStayIdempotentWhenRelationAlreadyActive() {
        SysUser targetUser = activeUser(12L);
        SysUserFollow relation = new SysUserFollow();
        relation.setId(20L);
        relation.setFollowerId(7L);
        relation.setFollowingId(12L);
        relation.setFollowStatus(1);

        when(sysUserRepository.getById(12L)).thenReturn(targetUser);
        when(sysUserFollowRepository.findByFollowerAndFollowing(7L, 12L)).thenReturn(relation);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userFollowService.followUser(12L);
        }

        verify(sysUserFollowRepository, never()).save(any(SysUserFollow.class));
        verify(sysUserFollowRepository, never()).updateById(any(SysUserFollow.class));
        verify(followNoticeService, never()).notifyNewFollowerAfterCommit(any(), any());
    }

    @Test
    void unfollowUserShouldMarkRelationInactive() {
        SysUser targetUser = activeUser(12L);
        SysUserFollow relation = new SysUserFollow();
        relation.setId(20L);
        relation.setFollowerId(7L);
        relation.setFollowingId(12L);
        relation.setFollowStatus(1);

        when(sysUserRepository.getById(12L)).thenReturn(targetUser);
        when(sysUserFollowRepository.findByFollowerAndFollowing(7L, 12L)).thenReturn(relation);
        when(sysUserFollowRepository.updateById(relation)).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userFollowService.unfollowUser(12L);
        }

        assertEquals(Integer.valueOf(0), relation.getFollowStatus());
        verify(sysUserFollowRepository).updateById(relation);
        verify(redisOperator).delete(RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 7L));
        verify(redisOperator).delete(RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 12L));
    }

    @Test
    void pageMyFollowsShouldReturnMappedRecords() {
        UserFollowPageQuery query = new UserFollowPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setSpecialOnly(true);

        FollowRelationUserItem item = new FollowRelationUserItem();
        item.setUserId(12L);
        item.setUsername("target-user");

        UserFollowUserVO vo = new UserFollowUserVO();
        vo.setUserId(12L);
        vo.setUsername("target-user");

        when(sysUserFollowRepository.countFollowPage(7L, true)).thenReturn(8L);
        when(sysUserFollowRepository.selectFollowPage(7L, true, 5L, 5L)).thenReturn(List.of(item));
        when(followModelConvert.toUserFollowUserVO(item)).thenReturn(vo);

        PageResult<UserFollowUserVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            result = userFollowService.pageMyFollows(query);
        }

        assertEquals(8L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertEquals("target-user", result.getRecords().get(0).getUsername());
    }

    @Test
    void getMutualFollowStatusShouldAssembleRelationFlags() {
        SysUser targetUser = activeUser(12L);
        when(sysUserRepository.getById(12L)).thenReturn(targetUser);
        when(sysUserFollowRepository.countActiveRelation(7L, 12L)).thenReturn(1L);
        when(sysUserFollowRepository.countActiveRelation(12L, 7L)).thenReturn(0L);

        UserFollowMutualVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            result = userFollowService.getMutualFollowStatus(12L);
        }

        assertTrue(result.getFollowing());
        assertFalse(result.getFollowedBy());
        assertFalse(result.getMutualFollow());
    }

    @Test
    void getMyFollowCountShouldUseMapperCounts() {
        String cacheKey = RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 7L);
        when(redisOperator.get(cacheKey, UserFollowCountVO.class)).thenReturn(null);
        when(sysUserFollowRepository.countActiveFollowing(7L)).thenReturn(3L);
        when(sysUserFollowRepository.countActiveFans(7L)).thenReturn(5L);

        UserFollowCountVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            result = userFollowService.getMyFollowCount();
        }

        assertEquals(3L, result.getFollowingCount());
        assertEquals(5L, result.getFanCount());
        verify(redisOperator).set(eq(cacheKey), eq(result), any(Duration.class));
    }

    @Test
    void getMyFollowCountShouldReturnCachedCountsWhenCacheHit() {
        String cacheKey = RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 7L);
        UserFollowCountVO cached = UserFollowCountVO.builder()
                .followingCount(9L)
                .fanCount(11L)
                .build();
        when(redisOperator.get(cacheKey, UserFollowCountVO.class)).thenReturn(cached);

        UserFollowCountVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            result = userFollowService.getMyFollowCount();
        }

        assertEquals(9L, result.getFollowingCount());
        assertEquals(11L, result.getFanCount());
        verify(sysUserFollowRepository, never()).countActiveFollowing(any());
        verify(sysUserFollowRepository, never()).countActiveFans(any());
    }

    @Test
    void getMyFollowCountShouldFallbackToRepositoryWhenRedisThrows() {
        String cacheKey = RedisKeyUtils.build(RedisConstants.FOLLOW_COUNT_CACHE_PREFIX, 7L);
        when(redisOperator.get(cacheKey, UserFollowCountVO.class)).thenThrow(new RuntimeException("redis down"));
        when(sysUserFollowRepository.countActiveFollowing(7L)).thenReturn(3L);
        when(sysUserFollowRepository.countActiveFans(7L)).thenReturn(5L);

        UserFollowCountVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            result = userFollowService.getMyFollowCount();
        }

        assertEquals(3L, result.getFollowingCount());
        assertEquals(5L, result.getFanCount());
    }

    @Test
    void updateRemarkShouldNormalizeAndPersistRelation() {
        SysUser targetUser = activeUser(12L);
        SysUserFollow relation = new SysUserFollow();
        relation.setId(20L);
        relation.setFollowerId(7L);
        relation.setFollowingId(12L);
        relation.setFollowStatus(1);

        UserFollowRemarkUpdateRequest request = new UserFollowRemarkUpdateRequest();
        request.setRemark(" 前端联调 ");

        when(sysUserRepository.getById(12L)).thenReturn(targetUser);
        when(sysUserFollowRepository.findByFollowerAndFollowing(7L, 12L)).thenReturn(relation);
        when(sysUserFollowRepository.updateById(relation)).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userFollowService.updateRemark(12L, request);
        }

        verify(followModelConvert).updateRemark(request, relation);
        verify(sysUserFollowRepository).updateById(relation);
    }

    @Test
    void updateSpecialFollowShouldPersistValidFlag() {
        SysUser targetUser = activeUser(12L);
        SysUserFollow relation = new SysUserFollow();
        relation.setId(20L);
        relation.setFollowerId(7L);
        relation.setFollowingId(12L);
        relation.setFollowStatus(1);

        UserFollowSpecialUpdateRequest request = new UserFollowSpecialUpdateRequest();
        request.setSpecialFollow(1);

        when(sysUserRepository.getById(12L)).thenReturn(targetUser);
        when(sysUserFollowRepository.findByFollowerAndFollowing(7L, 12L)).thenReturn(relation);
        when(sysUserFollowRepository.updateById(relation)).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userFollowService.updateSpecialFollow(12L, request);
        }

        verify(followModelConvert).updateSpecialFollow(request, relation);
        verify(sysUserFollowRepository).updateById(relation);
    }

    private SysUser activeUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(1);
        user.setDeletedFlag(0);
        return user;
    }
}
