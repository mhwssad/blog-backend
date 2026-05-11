package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.ai.model.user.AiQuotaVO;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.service.impl.AiQuotaServiceImpl;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiQuotaServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AiQuotaServiceImplTest {

    @Mock
    private RedisOperator redisOperator;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private UserExperienceService userExperienceService;
    @Mock
    private AiChannelConfigRepository aiChannelConfigRepository;

    private AiQuotaServiceImpl aiQuotaService;

    @BeforeEach
    void setUp() {
        aiQuotaService = new AiQuotaServiceImpl(
                redisOperator,
                sysConfigService,
                userExperienceService,
                aiChannelConfigRepository
        );
    }

    // ========== checkQuota ==========

    @Test
    void checkQuotaShouldRejectWhenGlobalDisabled() {
        Long userId = 1L;
        AiChannelConfig config = buildChannelConfig(10L, 20);

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.AI_GLOBAL_ENABLED_KEY, ConfigConstants.DEFAULT_AI_GLOBAL_ENABLED))
                .thenReturn("false");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiQuotaService.checkQuota(userId, config));

        assertEquals(ResultErrorCode.AI_GLOBAL_DISABLED.getCode(), exception.getCode());
        verify(redisOperator, never()).get(anyString());
    }

    @Test
    void checkQuotaShouldRejectWhenUserQuotaExceeded() {
        Long userId = 1L;
        AiChannelConfig config = buildChannelConfig(10L, 5);

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.AI_GLOBAL_ENABLED_KEY, ConfigConstants.DEFAULT_AI_GLOBAL_ENABLED))
                .thenReturn("true");
        when(sysConfigService.getValueOrDefault(
                eq(ConfigConstants.AI_PLATFORM_DAILY_QUOTA_KEY),
                any())).thenReturn("1000");
        // Platform used count
        when(redisOperator.get(anyString())).thenReturn(10L);
        // User level 1 -> levelQuota = 5
        when(userExperienceService.getUserLevel(userId)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiQuotaService.checkQuota(userId, config));

        assertEquals(ResultErrorCode.AI_QUOTA_EXCEEDED.getCode(), exception.getCode());
    }

    @Test
    void checkQuotaShouldPassWhenWithinQuota() {
        Long userId = 1L;
        AiChannelConfig config = buildChannelConfig(10L, 20);

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.AI_GLOBAL_ENABLED_KEY, ConfigConstants.DEFAULT_AI_GLOBAL_ENABLED))
                .thenReturn("true");
        when(sysConfigService.getValueOrDefault(
                eq(ConfigConstants.AI_PLATFORM_DAILY_QUOTA_KEY),
                any())).thenReturn("1000");
        // Platform used = 10, User used = 3
        when(redisOperator.get(anyString())).thenReturn(10L, 3L);
        when(userExperienceService.getUserLevel(userId)).thenReturn(1);

        assertDoesNotThrow(() -> aiQuotaService.checkQuota(userId, config));
    }

    @Test
    void checkQuotaShouldRejectWhenPlatformQuotaExceeded() {
        Long userId = 1L;
        AiChannelConfig config = buildChannelConfig(10L, 20);

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.AI_GLOBAL_ENABLED_KEY, ConfigConstants.DEFAULT_AI_GLOBAL_ENABLED))
                .thenReturn("true");
        when(sysConfigService.getValueOrDefault(
                eq(ConfigConstants.AI_PLATFORM_DAILY_QUOTA_KEY),
                any())).thenReturn("100");
        // Platform used = 100 (>= quota)
        when(redisOperator.get(anyString())).thenReturn(100L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiQuotaService.checkQuota(userId, config));

        assertEquals(ResultErrorCode.AI_QUOTA_PLATFORM_EXCEEDED.getCode(), exception.getCode());
    }

    // ========== recordUsage ==========

    @Test
    void recordUsageShouldIncrementCounters() {
        Long userId = 1L;
        Long channelConfigId = 10L;

        when(redisOperator.increment(anyString())).thenReturn(1L);

        aiQuotaService.recordUsage(userId, channelConfigId);

        // Should increment twice: platform key + user key
        verify(redisOperator, times(2)).increment(anyString());
        verify(redisOperator, times(2)).expire(anyString(), any(Duration.class));
    }

    // ========== computeEffectiveLimit (via getUserQuota) ==========

    @Test
    void computeEffectiveLimitShouldUseMinOfLevelAndChannelQuota() {
        Long userId = 1L;
        AiChannelConfig config = buildChannelConfig(10L, 5);

        // Level 3 -> levelQuota = 15; channelQuota = 5; effective = min(15, 5) = 5
        when(userExperienceService.getUserLevel(userId)).thenReturn(3);
        when(redisOperator.get(anyString())).thenReturn(null);

        AiQuotaVO result = aiQuotaService.getUserQuota(userId, config);

        assertEquals(5, result.getDailyLimit());
    }

    @Test
    void computeEffectiveLimitShouldUseChannelDefaultWhenNoLevelQuota() {
        Long userId = 1L;
        // channelQuota = 0 means no extra channel restriction
        AiChannelConfig config = buildChannelConfig(10L, 0);

        // Level 2 -> levelQuota = 10; channelQuota = 0 -> use levelQuota = 10
        when(userExperienceService.getUserLevel(userId)).thenReturn(2);
        when(redisOperator.get(anyString())).thenReturn(null);

        AiQuotaVO result = aiQuotaService.getUserQuota(userId, config);

        assertEquals(10, result.getDailyLimit());
    }

    // ========== Helper methods ==========

    private AiChannelConfig buildChannelConfig(Long id, int userDailyQuota) {
        AiChannelConfig config = new AiChannelConfig();
        config.setId(id);
        config.setUserDailyQuota(userDailyQuota);
        config.setStatus(1);
        return config;
    }
}
