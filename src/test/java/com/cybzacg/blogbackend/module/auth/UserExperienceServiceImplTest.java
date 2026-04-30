package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.UserExperienceLog;
import com.cybzacg.blogbackend.enums.experience.ExperienceSourceTypeEnum;
import com.cybzacg.blogbackend.module.auth.experience.constant.ExperienceConstants;
import com.cybzacg.blogbackend.module.auth.experience.event.XpAwardEvent;
import com.cybzacg.blogbackend.module.auth.experience.service.impl.UserExperienceServiceImpl;
import com.cybzacg.blogbackend.module.auth.experience.level.LevelConfig;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.experience.repository.UserExperienceLogRepository;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserExperienceServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserExperienceServiceImplTest {

    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private UserExperienceLogRepository experienceLogRepository;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private RedisOperator redisOperator;

    private UserExperienceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserExperienceServiceImpl(
                sysUserRepository,
                experienceLogRepository,
                sysConfigService,
                redisOperator
        );
    }

    @Test
    void awardExperienceShouldGrantDailyLoginOnlyOncePerDay() {
        XpAwardEvent event = dailyLoginEvent(1L, "daily-login-20260429");
        String idempotentKey = RedisKeyUtils.build(
                RedisConstants.XP_IDEMPOTENT_KEY_PREFIX, event.getIdempotentKey());

        // 第一次调用：幂等键不存在，正常发放
        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigEnabledKey(), "1")).thenReturn("1");
        when(redisOperator.setIfAbsent(eq(idempotentKey), eq("1"), any(Duration.class))).thenReturn(true);
        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigValueKey(),
                String.valueOf(ConfigConstants.DEFAULT_XP_SOURCE_DAILY_LOGIN_VALUE)))
                .thenReturn("10");
        // 每日上限未达
        when(sysConfigService.getValueOrDefault(
                ConfigConstants.XP_DAILY_TOTAL_CAP_KEY,
                String.valueOf(ConfigConstants.DEFAULT_XP_DAILY_TOTAL_CAP)))
                .thenReturn("200");
        when(redisOperator.get(contains("total"))).thenReturn(null);
        when(sysUserRepository.incrementExperiencePoints(1L, 10)).thenReturn(1);

        SysUser user = buildUser(1L, 10, 1);
        when(sysUserRepository.getById(1L)).thenReturn(user);

        service.awardExperience(event);

        verify(experienceLogRepository).save(any(UserExperienceLog.class));
        verify(sysUserRepository).incrementExperiencePoints(1L, 10);

        // 第二次调用：幂等键已存在，跳过
        reset(experienceLogRepository, sysUserRepository);
        when(redisOperator.setIfAbsent(eq(idempotentKey), eq("1"), any(Duration.class))).thenReturn(false);

        service.awardExperience(event);

        verify(experienceLogRepository, never()).save(any());
        verify(sysUserRepository, never()).incrementExperiencePoints(anyLong(), anyInt());
    }

    @Test
    void awardExperienceShouldStopAtPerSourceCap() {
        // 使用 COMMENT_CREATE 类型，它有 dailyCapConfigKey
        XpAwardEvent event = new XpAwardEvent(1L, "comment_create", "comment-100", "idem-comment-100");
        String idempotentKey = RedisKeyUtils.build(
                RedisConstants.XP_IDEMPOTENT_KEY_PREFIX, event.getIdempotentKey());

        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.COMMENT_CREATE.getConfigEnabledKey(), "1")).thenReturn("1");
        when(redisOperator.setIfAbsent(eq(idempotentKey), eq("1"), any(Duration.class))).thenReturn(true);
        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.COMMENT_CREATE.getConfigValueKey(),
                String.valueOf(ConfigConstants.DEFAULT_XP_SOURCE_COMMENT_CREATE_VALUE)))
                .thenReturn("5");
        // 每日总量上限未达
        when(sysConfigService.getValueOrDefault(
                ConfigConstants.XP_DAILY_TOTAL_CAP_KEY,
                String.valueOf(ConfigConstants.DEFAULT_XP_DAILY_TOTAL_CAP)))
                .thenReturn("200");
        when(redisOperator.get(contains("total"))).thenReturn(null);
        // 单来源上限已达到：已获得 50，上限 50，再加 5 超过
        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.COMMENT_CREATE.getDailyCapConfigKey(),
                String.valueOf(Integer.MAX_VALUE)))
                .thenReturn("50");
        String sourceKey = RedisKeyUtils.build(
                RedisConstants.XP_DAILY_KEY_PREFIX, "1", LocalDate.now().toString(), "comment_create");
        when(redisOperator.get(sourceKey)).thenReturn("48");

        service.awardExperience(event);

        // 48 + 5 > 50，source cap reached，不应入账
        verify(sysUserRepository, never()).incrementExperiencePoints(anyLong(), anyInt());
        verify(experienceLogRepository, never()).save(any());
    }

    @Test
    void awardExperienceShouldStopAtDailyTotalCap() {
        XpAwardEvent event = dailyLoginEvent(1L, "daily-login-cap");
        String idempotentKey = RedisKeyUtils.build(
                RedisConstants.XP_IDEMPOTENT_KEY_PREFIX, event.getIdempotentKey());

        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigEnabledKey(), "1")).thenReturn("1");
        when(redisOperator.setIfAbsent(eq(idempotentKey), eq("1"), any(Duration.class))).thenReturn(true);
        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigValueKey(),
                String.valueOf(ConfigConstants.DEFAULT_XP_SOURCE_DAILY_LOGIN_VALUE)))
                .thenReturn("10");
        // 每日总量上限 200，已获得 195，再加 10 超过
        when(sysConfigService.getValueOrDefault(
                ConfigConstants.XP_DAILY_TOTAL_CAP_KEY,
                String.valueOf(ConfigConstants.DEFAULT_XP_DAILY_TOTAL_CAP)))
                .thenReturn("200");
        when(redisOperator.get(contains("total"))).thenReturn("195");

        service.awardExperience(event);

        verify(sysUserRepository, never()).incrementExperiencePoints(anyLong(), anyInt());
        verify(experienceLogRepository, never()).save(any());
    }

    @Test
    void awardExperienceShouldSkipOnIdempotentKeyDuplicate() {
        // 幂等 Redis 检查通过，但 DB 层 DuplicateKey 兜底
        XpAwardEvent event = dailyLoginEvent(1L, "dup-key-test");
        String idempotentKey = RedisKeyUtils.build(
                RedisConstants.XP_IDEMPOTENT_KEY_PREFIX, event.getIdempotentKey());

        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigEnabledKey(), "1")).thenReturn("1");
        when(redisOperator.setIfAbsent(eq(idempotentKey), eq("1"), any(Duration.class))).thenReturn(true);
        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigValueKey(),
                String.valueOf(ConfigConstants.DEFAULT_XP_SOURCE_DAILY_LOGIN_VALUE)))
                .thenReturn("10");
        when(sysConfigService.getValueOrDefault(
                ConfigConstants.XP_DAILY_TOTAL_CAP_KEY,
                String.valueOf(ConfigConstants.DEFAULT_XP_DAILY_TOTAL_CAP)))
                .thenReturn("200");
        when(redisOperator.get(contains("total"))).thenReturn(null);
        // DB 保存抛出 DuplicateKeyException
        doThrow(new DuplicateKeyException("uk_idempotent_key")).when(experienceLogRepository).save(any());

        service.awardExperience(event);

        verify(sysUserRepository, never()).incrementExperiencePoints(anyLong(), anyInt());
    }

    @Test
    void awardExperienceShouldLevelUpImmediatelyOnThreshold() {
        // 用户在 level 1（0 XP），给予 100 XP 应升到 level 2
        XpAwardEvent event = dailyLoginEvent(1L, "level-up-test");
        String idempotentKey = RedisKeyUtils.build(
                RedisConstants.XP_IDEMPOTENT_KEY_PREFIX, event.getIdempotentKey());

        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigEnabledKey(), "1")).thenReturn("1");
        when(redisOperator.setIfAbsent(eq(idempotentKey), eq("1"), any(Duration.class))).thenReturn(true);
        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigValueKey(),
                String.valueOf(ConfigConstants.DEFAULT_XP_SOURCE_DAILY_LOGIN_VALUE)))
                .thenReturn("100");
        when(sysConfigService.getValueOrDefault(
                ConfigConstants.XP_DAILY_TOTAL_CAP_KEY,
                String.valueOf(ConfigConstants.DEFAULT_XP_DAILY_TOTAL_CAP)))
                .thenReturn("200");
        when(redisOperator.get(contains("total"))).thenReturn(null);
        when(sysUserRepository.incrementExperiencePoints(1L, 100)).thenReturn(1);

        // 用户当前 0 XP，level 1。发放后 DB 中 experiencePoints 还是 0（还没刷新），
        // 但 calculateLevel 使用 user.getExperiencePoints() + xpValue
        SysUser user = buildUser(1L, 0, 1);
        when(sysUserRepository.getById(1L)).thenReturn(user);

        service.awardExperience(event);

        verify(sysUserRepository).updateLevel(1L, 2);
    }

    @Test
    void awardExperienceShouldNotAutoDowngradeInPhase1() {
        // 用户 level 5（4000+ XP），本次发放的 XP 不会降级
        // 由于 LevelCalculator 基于总经验值，总经验不变则等级不变。
        // 测试：用户已经 level 5，当前经验 4000，但 getById 返回时 level 仍是 5，
        // calculateLevel(4000) = 5，newLevel == userLevel，不触发 updateLevel
        XpAwardEvent event = dailyLoginEvent(1L, "no-downgrade-test");
        String idempotentKey = RedisKeyUtils.build(
                RedisConstants.XP_IDEMPOTENT_KEY_PREFIX, event.getIdempotentKey());

        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigEnabledKey(), "1")).thenReturn("1");
        when(redisOperator.setIfAbsent(eq(idempotentKey), eq("1"), any(Duration.class))).thenReturn(true);
        when(sysConfigService.getValueOrDefault(
                ExperienceSourceTypeEnum.DAILY_LOGIN.getConfigValueKey(),
                String.valueOf(ConfigConstants.DEFAULT_XP_SOURCE_DAILY_LOGIN_VALUE)))
                .thenReturn("10");
        when(sysConfigService.getValueOrDefault(
                ConfigConstants.XP_DAILY_TOTAL_CAP_KEY,
                String.valueOf(ConfigConstants.DEFAULT_XP_DAILY_TOTAL_CAP)))
                .thenReturn("200");
        when(redisOperator.get(contains("total"))).thenReturn(null);
        when(sysUserRepository.incrementExperiencePoints(1L, 10)).thenReturn(1);

        // 用户当前 4000 XP，level 5
        SysUser user = buildUser(1L, 4000, 5);
        when(sysUserRepository.getById(1L)).thenReturn(user);

        service.awardExperience(event);

        // 4000 + 10 = 4010, calculateLevel(4010) = 5, 和当前 level 5 相同，不更新
        verify(sysUserRepository, never()).updateLevel(anyLong(), anyInt());
    }

    // ===================== helpers =====================

    private XpAwardEvent dailyLoginEvent(Long userId, String idempotentKey) {
        return new XpAwardEvent(userId, "daily_login", "biz-" + idempotentKey, idempotentKey);
    }

    private SysUser buildUser(Long id, int experiencePoints, int userLevel) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setExperiencePoints(experiencePoints);
        user.setUserLevel(userLevel);
        return user;
    }
}
