package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelAccount;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelAccountRepository;
import com.cybzacg.blogbackend.module.ai.service.AiAccountPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 账号池服务实现。
 *
 * <p>按额度选号，权重轮询分发，连续错误自动禁用，到期自动恢复。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAccountPoolServiceImpl implements AiAccountPoolService {

    private static final int AUTO_RECOVER_MINUTES = 10;

    private final AiChannelAccountRepository aiChannelAccountRepository;
    private final RedisOperator redisOperator;

    private final ConcurrentHashMap<Long, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    @Override
    public AiChannelAccount selectAccount(AiChannelConfig config) {
        List<AiChannelAccount> enabled = aiChannelAccountRepository.listEnabledByChannelId(config.getId());
        if (enabled.isEmpty()) {
            return null;
        }

        // 过滤掉额度已耗尽的账号
        List<AiChannelAccount> available = new ArrayList<>();
        for (AiChannelAccount account : enabled) {
            if (!isQuotaExhausted(account)) {
                available.add(account);
            }
        }

        if (available.isEmpty()) {
            return null;
        }

        if (available.size() == 1) {
            return available.get(0);
        }

        // 按权重轮询
        return weightedRoundRobin(config.getId(), available);
    }

    @Override
    public void reportSuccess(Long accountId) {
        aiChannelAccountRepository.resetErrors(accountId);
        incrementTodayUsage(accountId);
    }

    @Override
    public void reportFailure(Long accountId, String errorMessage) {
        aiChannelAccountRepository.incrementErrors(accountId, errorMessage);
        log.warn("AI 账号调用失败, accountId={}: {}", accountId, errorMessage);
    }

    @Override
    public void recoverDisabledAccounts() {
        int recovered = aiChannelAccountRepository.recoverDisabledAccounts();
        if (recovered > 0) {
            log.info("AI 账号池自动恢复 {} 个账号", recovered);
        }
    }

    @Override
    public long getTodayUsage(Long accountId) {
        String key = RedisKeyUtils.build(RedisConstants.AI_ACCOUNT_POOL_USAGE_PREFIX, accountId, today());
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

    @Override
    public void incrementTodayUsage(Long accountId) {
        String key = RedisKeyUtils.build(RedisConstants.AI_ACCOUNT_POOL_USAGE_PREFIX, accountId, today());
        long newVal = redisOperator.increment(key);
        if (newVal == 1L) {
            redisOperator.expire(key, durationUntilMidnight());
        }
    }

    private boolean isQuotaExhausted(AiChannelAccount account) {
        int quota = account.getDailyQuota() != null ? account.getDailyQuota() : 0;
        if (quota <= 0) {
            return false;
        }
        long usage = getTodayUsage(account.getId());
        return usage >= quota;
    }

    private AiChannelAccount weightedRoundRobin(Long channelId, List<AiChannelAccount> accounts) {
        // 构建权重展开列表
        List<AiChannelAccount> expanded = new ArrayList<>();
        for (AiChannelAccount account : accounts) {
            int weight = account.getWeight() != null && account.getWeight() > 0 ? account.getWeight() : 1;
            for (int i = 0; i < weight; i++) {
                expanded.add(account);
            }
        }

        AtomicInteger counter = roundRobinCounters.computeIfAbsent(channelId, k -> new AtomicInteger(0));
        int index = Math.abs(counter.getAndIncrement() % expanded.size());
        return expanded.get(index);
    }

    private String today() {
        return LocalDate.now().toString();
    }

    private Duration durationUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        Duration duration = Duration.between(now, midnight);
        return duration.isZero() || duration.isNegative() ? Duration.ofSeconds(1) : duration;
    }
}
