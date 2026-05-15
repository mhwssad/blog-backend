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

    /**
     * 从渠道对应的可用账号池中选择一个账号。
     *
     * <p>选择策略：先过滤掉额度已耗尽的账号，若剩余账号数 > 1 则按权重轮询分发。
     *
     * @param config 渠道配置，用于确定候选账号范围
     * @return 选中的账号，若全部不可用则返回 null
     */
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

    /**
     * 上报账号调用成功，重置连续错误计数并增加当日使用量。
     *
     * @param accountId 账号 ID
     */
    @Override
    public void reportSuccess(Long accountId) {
        aiChannelAccountRepository.resetErrors(accountId);
        incrementTodayUsage(accountId);
    }

    /**
     * 上报账号调用失败，累加连续错误计数，达到上限后自动禁用该账号。
     *
     * @param accountId   账号 ID
     * @param errorMessage 失败原因
     */
    @Override
    public void reportFailure(Long accountId, String errorMessage) {
        aiChannelAccountRepository.incrementErrors(accountId, errorMessage);
        log.warn("AI 账号调用失败, accountId={}: {}", accountId, errorMessage);
    }

    /**
     * 自动恢复因连续错误被禁用且已超过恢复等待期（{@value #AUTO_RECOVER_MINUTES} 分钟）的账号。
     *
     * <p>通常由定时任务周期性调用。
     */
    @Override
    public void recoverDisabledAccounts() {
        int recovered = aiChannelAccountRepository.recoverDisabledAccounts();
        if (recovered > 0) {
            log.info("AI 账号池自动恢复 {} 个账号", recovered);
        }
    }

    /**
     * 获取指定账号当日已使用的调用次数。
     *
     * @param accountId 账号 ID
     * @return 当日使用次数
     */
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

    /**
     * 递增指定账号的当日使用计数，首次写入时设置过期时间到当日午夜自动清除。
     *
     * @param accountId 账号 ID
     */
    @Override
    public void incrementTodayUsage(Long accountId) {
        String key = RedisKeyUtils.build(RedisConstants.AI_ACCOUNT_POOL_USAGE_PREFIX, accountId, today());
        long newVal = redisOperator.increment(key);
        if (newVal == 1L) {
            redisOperator.expire(key, durationUntilMidnight());
        }
    }

    /**
     * 判断账号当日额度是否已耗尽。
     *
     * <p>当日未配置额度上限（quota ≤ 0）视为不限制。
     */
    private boolean isQuotaExhausted(AiChannelAccount account) {
        int quota = account.getDailyQuota() != null ? account.getDailyQuota() : 0;
        if (quota <= 0) {
            return false;
        }
        long usage = getTodayUsage(account.getId());
        return usage >= quota;
    }

    /**
     * 按权重展开账号列表后进行轮询选择，权重越大的账号出现次数越多。
     *
     * @param channelId 渠道 ID，用于维护独立的轮询计数器
     * @param accounts  候选账号列表
     * @return 轮询选中的账号
     */
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

    /** 获取当前日期的字符串表示，用于构建 Redis Key 中的日期部分。 */
    private String today() {
        return LocalDate.now().toString();
    }

    /**
     * 计算从当前时刻到次日午夜的时间间隔，用于设置 Redis Key 的 TTL。
     *
     * <p>若计算结果为零或负数（极端边界），返回 1 秒避免 TTL 为零。
     */
    private Duration durationUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        Duration duration = Duration.between(now, midnight);
        return duration.isZero() || duration.isNegative() ? Duration.ofSeconds(1) : duration;
    }
}
