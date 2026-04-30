package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageGovernanceService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 聊天消息治理实现。
 *
 * <p>当前先收口为：
 * 用户维度分钟级发送频控，以及基于系统配置的敏感词匹配。
 */
@Service
public class ChatMessageGovernanceServiceImpl implements ChatMessageGovernanceService {
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60L;

    private final RedisOperator redisOperator;
    private final SysConfigService sysConfigService;
    private final MeterRegistry meterRegistry;

    public ChatMessageGovernanceServiceImpl(RedisOperator redisOperator,
                                            SysConfigService sysConfigService,
                                            MeterRegistry meterRegistry) {
        this.redisOperator = redisOperator;
        this.sysConfigService = sysConfigService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 校验文本消息的发送频率和敏感词。
     *
     * @param userId  发送者用户 ID
     * @param content 消息文本内容
     */
    @Override
    public void validateTextMessage(Long userId, String content) {
        enforceUserRateLimit(userId);
        validateSensitiveWords(content);
    }

    /**
     * 校验附件消息的发送频率（仅频控，不含内容审查）。
     *
     * @param userId 发送者用户 ID
     */
    @Override
    public void validateAttachmentMessage(Long userId) {
        enforceUserRateLimit(userId);
    }

    /**
     * 聊天域单独按“用户 + 分钟窗口”限流，避免只有全局 IP 过滤器而没有账号级兜底。
     */
    private void enforceUserRateLimit(Long userId) {
        if (userId == null) {
            return;
        }
        int limit = resolveRateLimitPerMinute();
        if (limit <= 0) {
            return;
        }
        try {
            long window = Instant.now().getEpochSecond() / RATE_LIMIT_WINDOW_SECONDS;
            String rateKey = RedisKeyUtils.build(RedisConstants.CHAT_SEND_RATE_LIMIT_KEY_PREFIX, userId, window);
            long requestCount = redisOperator.increment(rateKey);
            if (requestCount == 1L) {
                redisOperator.expire(rateKey, RATE_LIMIT_WINDOW_SECONDS + 5L, TimeUnit.SECONDS);
            }
            if (requestCount > limit) {
                meterRegistry.counter("chat.message.governance.reject.total", "reason", "rate_limit").increment();
                ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.REQUEST_RATE_LIMITED, "聊天发送过于频繁，请稍后再试");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            meterRegistry.counter("chat.message.governance.error.total", "type", "rate_limit").increment();
        }
    }

    /**
     * 使用系统配置维护聊天敏感词，先按包含匹配收口基础拦截，后续再扩展更复杂审核链路。
     */
    private void validateSensitiveWords(String content) {
        String normalizedContent = StrUtils.trimToNull(content);
        if (normalizedContent == null) {
            return;
        }
        String lowerContent = normalizedContent.toLowerCase(Locale.ROOT);
        for (String sensitiveWord : resolveSensitiveWords()) {
            if (lowerContent.contains(sensitiveWord.toLowerCase(Locale.ROOT))) {
                meterRegistry.counter("chat.message.governance.reject.total", "reason", "sensitive_word").increment();
                ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "消息内容包含敏感词，请调整后再发送");
            }
        }
    }

    private int resolveRateLimitPerMinute() {
        String configValue = sysConfigService.getValueOrDefault(
                ConfigConstants.CHAT_SEND_RATE_LIMIT_PER_MINUTE_KEY,
                String.valueOf(ConfigConstants.DEFAULT_CHAT_SEND_RATE_LIMIT_PER_MINUTE));
        String normalizedValue = StrUtils.trimToNull(configValue);
        if (normalizedValue == null) {
            return ConfigConstants.DEFAULT_CHAT_SEND_RATE_LIMIT_PER_MINUTE;
        }
        try {
            return Integer.parseInt(normalizedValue);
        } catch (NumberFormatException ex) {
            return ConfigConstants.DEFAULT_CHAT_SEND_RATE_LIMIT_PER_MINUTE;
        }
    }

    private List<String> resolveSensitiveWords() {
        String configValue = sysConfigService.getValueOrDefault(ConfigConstants.CHAT_SENSITIVE_WORDS_KEY, "");
        String normalizedValue = StrUtils.trimToNull(configValue);
        if (normalizedValue == null) {
            return List.of();
        }
        return Arrays.stream(normalizedValue.split("[,\\n\\r]+"))
                .map(StrUtils::trimToNull)
                .filter(StrUtils::hasText)
                .distinct()
                .toList();
    }
}
