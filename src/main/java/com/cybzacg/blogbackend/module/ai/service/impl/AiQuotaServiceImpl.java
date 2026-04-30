package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.model.user.AiQuotaVO;
import com.cybzacg.blogbackend.module.ai.repository.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.module.auth.experience.level.LevelConfig;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * AI 配额服务实现。
 *
 * <p>基于 Redis 计数器实现平台/用户每日额度校验与记录，自动处理 Key 过期。
 */
@Service
@RequiredArgsConstructor
public class AiQuotaServiceImpl implements AiQuotaService {

    private final RedisOperator redisOperator;
    private final SysConfigService sysConfigService;
    private final UserExperienceService userExperienceService;
    private final AiChannelConfigRepository aiChannelConfigRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkQuota(Long userId, AiChannelConfig config) {
        // 1. 全局开关
        String globalEnabled = sysConfigService.getValueOrDefault(
                ConfigConstants.AI_GLOBAL_ENABLED_KEY, ConfigConstants.DEFAULT_AI_GLOBAL_ENABLED);
        ExceptionThrowerCore.throwBusinessIf(
                !"true".equalsIgnoreCase(globalEnabled), ResultErrorCode.AI_GLOBAL_DISABLED);

        // 2. 平台每日额度
        int platformQuota = Integer.parseInt(sysConfigService.getValueOrDefault(
                ConfigConstants.AI_PLATFORM_DAILY_QUOTA_KEY,
                String.valueOf(ConfigConstants.DEFAULT_AI_PLATFORM_DAILY_QUOTA)));
        if (platformQuota > 0) {
            String platformKey = RedisKeyUtils.build(RedisConstants.AI_QUOTA_PLATFORM_DAILY_PREFIX, today());
            long platformUsed = getCountWithInit(platformKey);
            ExceptionThrowerCore.throwBusinessIf(
                    platformUsed >= platformQuota, ResultErrorCode.AI_QUOTA_PLATFORM_EXCEEDED);
        }

        // 3. 用户每日额度
        int effectiveLimit = computeEffectiveLimit(userId, config);
        String userKey = RedisKeyUtils.build(RedisConstants.AI_QUOTA_USER_DAILY_PREFIX, userId, today());
        long userUsed = getCountWithInit(userKey);
        ExceptionThrowerCore.throwBusinessIf(
                userUsed >= effectiveLimit, ResultErrorCode.AI_QUOTA_EXCEEDED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordUsage(Long userId, Long channelConfigId) {
        // 平台计数
        String platformKey = RedisKeyUtils.build(RedisConstants.AI_QUOTA_PLATFORM_DAILY_PREFIX, today());
        incrementWithTtl(platformKey);

        // 用户计数
        String userKey = RedisKeyUtils.build(RedisConstants.AI_QUOTA_USER_DAILY_PREFIX, userId, today());
        incrementWithTtl(userKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AiQuotaVO getUserQuota(Long userId, AiChannelConfig config) {
        int effectiveLimit = computeEffectiveLimit(userId, config);
        String userKey = RedisKeyUtils.build(RedisConstants.AI_QUOTA_USER_DAILY_PREFIX, userId, today());
        long usedToday = getCountWithInit(userKey);
        long remainingToday = Math.max(0, effectiveLimit - usedToday);

        AiQuotaVO vo = new AiQuotaVO();
        vo.setDailyLimit(effectiveLimit);
        vo.setUsedToday(usedToday);
        vo.setRemainingToday(remainingToday);
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AiQuotaVO getUserQuotaForDefaultChannel(Long userId) {
        List<AiChannelConfig> enabled = aiChannelConfigRepository.listEnabledOrderByDefault();
        if (enabled.isEmpty()) {
            AiQuotaVO vo = new AiQuotaVO();
            vo.setDailyLimit(0);
            vo.setUsedToday(0);
            vo.setRemainingToday(0);
            return vo;
        }
        return getUserQuota(userId, enabled.get(0));
    }

    /**
     * 计算用户有效每日额度上限。
     *
     * <p>取用户等级额度与渠道配置额度的较小值；渠道额度为 0 表示不额外限制。
     */
    private int computeEffectiveLimit(Long userId, AiChannelConfig config) {
        int userLevel = userExperienceService.getUserLevel(userId);
        LevelConfig levelConfig = LevelConfig.getByLevel(userLevel);
        int levelQuota = levelConfig != null ? levelConfig.getAiDailyQuota() : 5;

        int channelQuota = config.getUserDailyQuota() != null ? config.getUserDailyQuota() : 0;
        return channelQuota > 0 ? Math.min(levelQuota, channelQuota) : levelQuota;
    }

    /**
     * 递增计数器，首次写入时设置 TTL 到当日午夜。
     */
    private void incrementWithTtl(String key) {
        long newVal = redisOperator.increment(key);
        if (newVal == 1L) {
            redisOperator.expire(key, durationUntilMidnight());
        }
    }

    /**
     * 获取当前计数，Key 不存在时初始化为 0 并设置 TTL。
     */
    private long getCountWithInit(String key) {
        Object val = redisOperator.get(key);
        if (val == null) {
            return 0L;
        }
        if (val instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 获取当日日期字符串（yyyy-MM-dd）。
     */
    private String today() {
        return LocalDate.now().toString();
    }

    /**
     * 计算从现在到当日午夜的 Duration。
     */
    private Duration durationUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        Duration duration = Duration.between(now, midnight);
        // 保证至少 1 秒
        return duration.isZero() || duration.isNegative() ? Duration.ofSeconds(1) : duration;
    }
}
