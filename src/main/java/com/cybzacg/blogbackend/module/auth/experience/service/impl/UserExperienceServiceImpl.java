package com.cybzacg.blogbackend.module.auth.experience.service.impl;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.UserExperienceLog;
import com.cybzacg.blogbackend.enums.experience.ExperienceSourceTypeEnum;
import com.cybzacg.blogbackend.module.auth.experience.constant.ExperienceConstants;
import com.cybzacg.blogbackend.module.auth.experience.event.XpAwardEvent;
import com.cybzacg.blogbackend.module.auth.experience.level.LevelCalculator;
import com.cybzacg.blogbackend.module.auth.experience.level.LevelConfig;
import com.cybzacg.blogbackend.module.auth.experience.model.user.UserLevelInfoVO;
import com.cybzacg.blogbackend.module.auth.experience.repository.UserExperienceLogRepository;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * 用户经验服务实现。
 *
 * <p>监听 XpAwardEvent，执行幂等检查、每日上限校验、经验入账和等级计算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserExperienceServiceImpl implements UserExperienceService {

    private final SysUserRepository sysUserRepository;
    private final UserExperienceLogRepository experienceLogRepository;
    private final SysConfigService sysConfigService;
    private final RedisOperator redisOperator;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void awardExperience(XpAwardEvent event) {
        ExperienceSourceTypeEnum sourceType = ExperienceSourceTypeEnum.fromValue(event.getSourceType());
        if (sourceType == null) {
            log.warn("未知的经验来源类型: {}", event.getSourceType());
            return;
        }

        // 1. 检查来源是否启用
        if (!isSourceEnabled(sourceType)) {
            return;
        }

        // 2. Redis 幂等检查
        String idempotentRedisKey = RedisKeyUtils.build(
                RedisConstants.XP_IDEMPOTENT_KEY_PREFIX, event.getIdempotentKey());
        boolean isNew = redisOperator.setIfAbsent(idempotentRedisKey, "1",
                Duration.ofHours(ExperienceConstants.XP_IDEMPOTENT_TTL_HOURS));
        if (!isNew) {
            return;
        }

        // 3. 读取经验值配置
        int xpValue = getConfigInt(sourceType.getConfigValueKey(), getDefaultXpValue(sourceType));
        if (xpValue <= 0) {
            return;
        }

        // 4. 每日上限检查
        LocalDate today = LocalDate.now();
        if (isDailyCapReached(event.getUserId(), today, sourceType, xpValue)) {
            return;
        }

        // 5. 保存流水（幂等兜底）
        UserExperienceLog logEntry = new UserExperienceLog();
        logEntry.setUserId(event.getUserId());
        logEntry.setSourceType(sourceType.getValue());
        logEntry.setSourceBizId(event.getSourceBizId());
        logEntry.setXpValue(xpValue);
        logEntry.setIdempotentKey(event.getIdempotentKey());
        logEntry.setLogDate(today);
        try {
            experienceLogRepository.save(logEntry);
        } catch (DuplicateKeyException e) {
            return;
        }

        // 6. 原子更新经验值
        sysUserRepository.incrementExperiencePoints(event.getUserId(), xpValue);

        // 7. 等级计算与更新
        SysUser user = sysUserRepository.getById(event.getUserId());
        if (user != null) {
            int newLevel = LevelCalculator.calculateLevel(user.getExperiencePoints() + xpValue);
            if (newLevel != user.getUserLevel()) {
                sysUserRepository.updateLevel(event.getUserId(), newLevel);
            }
        }

        // 8. Redis 每日计数器
        incrementDailyCount(event.getUserId(), today, xpValue);
        if (sourceType.getDailyCapConfigKey() != null) {
            incrementDailySourceCount(event.getUserId(), today, sourceType.getValue(), xpValue);
        }
    }

    @Override
    public int getUserLevel(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        if (user == null) {
            return 1;
        }
        return user.getUserLevel() != null ? user.getUserLevel() : 1;
    }

    @Override
    public UserLevelInfoVO getLevelInfo(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        int level = user != null && user.getUserLevel() != null ? user.getUserLevel() : 1;
        int xp = user != null && user.getExperiencePoints() != null ? user.getExperiencePoints() : 0;
        LevelConfig currentConfig = LevelConfig.getByLevel(level);
        LevelConfig nextConfig = LevelConfig.getByLevel(Math.min(level + 1, 10));
        return UserLevelInfoVO.builder()
                .level(level)
                .title(currentConfig != null ? currentConfig.getTitle() : "")
                .experiencePoints(xp)
                .currentLevelThreshold(currentConfig != null ? currentConfig.getXpThreshold() : 0)
                .nextLevelThreshold(level < 10 && nextConfig != null ? nextConfig.getXpThreshold() : null)
                .build();
    }

    @Override
    public boolean checkLevelPermission(Long userId, int requiredLevel) {
        return getUserLevel(userId) >= requiredLevel;
    }

    private boolean isSourceEnabled(ExperienceSourceTypeEnum sourceType) {
        String enabled = sysConfigService.getValueOrDefault(sourceType.getConfigEnabledKey(), "1");
        return "1".equals(enabled);
    }

    private int getConfigInt(String key, int defaultValue) {
        String value = sysConfigService.getValueOrDefault(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getDefaultXpValue(ExperienceSourceTypeEnum sourceType) {
        return switch (sourceType) {
            case DAILY_LOGIN -> ConfigConstants.DEFAULT_XP_SOURCE_DAILY_LOGIN_VALUE;
            case ARTICLE_PUBLISH -> ConfigConstants.DEFAULT_XP_SOURCE_ARTICLE_PUBLISH_VALUE;
            case COMMENT_CREATE -> ConfigConstants.DEFAULT_XP_SOURCE_COMMENT_CREATE_VALUE;
            case LIKE_GIVEN -> ConfigConstants.DEFAULT_XP_SOURCE_LIKE_GIVEN_VALUE;
            case LIKE_RECEIVED -> ConfigConstants.DEFAULT_XP_SOURCE_LIKE_RECEIVED_VALUE;
            case CHAT_MESSAGE -> ConfigConstants.DEFAULT_XP_SOURCE_CHAT_MESSAGE_VALUE;
        };
    }

    private boolean isDailyCapReached(Long userId, LocalDate date,
                                       ExperienceSourceTypeEnum sourceType, int xpValue) {
        int totalCap = getConfigInt(ConfigConstants.XP_DAILY_TOTAL_CAP_KEY,
                ConfigConstants.DEFAULT_XP_DAILY_TOTAL_CAP);

        String totalKey = RedisKeyUtils.build(
                RedisConstants.XP_DAILY_KEY_PREFIX, String.valueOf(userId), date.toString(), "total");
        Object totalObj = redisOperator.get(totalKey);
        int currentTotal = 0;
        if (totalObj != null) {
            try {
                currentTotal = Integer.parseInt(totalObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        if (currentTotal + xpValue > totalCap) {
            return true;
        }

        if (sourceType.getDailyCapConfigKey() != null) {
            int sourceCap = getConfigInt(sourceType.getDailyCapConfigKey(), Integer.MAX_VALUE);
            if (sourceCap < Integer.MAX_VALUE) {
                String sourceKey = RedisKeyUtils.build(
                        RedisConstants.XP_DAILY_KEY_PREFIX, String.valueOf(userId),
                        date.toString(), sourceType.getValue());
                Object sourceObj = redisOperator.get(sourceKey);
                int currentSource = 0;
                if (sourceObj != null) {
                    try {
                        currentSource = Integer.parseInt(sourceObj.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (currentSource + xpValue > sourceCap) {
                    return true;
                }
            }
        }

        return false;
    }

    private void incrementDailyCount(Long userId, LocalDate date, int xpValue) {
        String key = RedisKeyUtils.build(
                RedisConstants.XP_DAILY_KEY_PREFIX, String.valueOf(userId), date.toString(), "total");
        redisOperator.increment(key, xpValue);
        ensureTtlToEndOfDay(key, date);
    }

    private void incrementDailySourceCount(Long userId, LocalDate date, String sourceType, int xpValue) {
        String key = RedisKeyUtils.build(
                RedisConstants.XP_DAILY_KEY_PREFIX, String.valueOf(userId),
                date.toString(), sourceType);
        redisOperator.increment(key, xpValue);
        ensureTtlToEndOfDay(key, date);
    }

    private void ensureTtlToEndOfDay(String key, LocalDate date) {
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        long secondsRemaining = LocalDateTime.now().until(endOfDay, ChronoUnit.SECONDS);
        if (secondsRemaining > 0) {
            redisOperator.setIfAbsent(key + ":ttl", "1",
                    Duration.ofSeconds(Math.max(secondsRemaining, 60)));
        }
    }
}
