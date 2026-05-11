package com.cybzacg.blogbackend.scheduler;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.module.ai.service.AiAccountPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 账号池自动恢复调度器。
 *
 * <p>每分钟检查被自动禁用的账号，恢复已过冷却期的账号。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAccountPoolRecoveryScheduler {

    private final AiAccountPoolService aiAccountPoolService;
    private final RedisOperator redisOperator;

    @Scheduled(fixedDelay = 60_000)
    public void recover() {
        String lockKey = RedisKeyUtils.build(RedisConstants.AI_ACCOUNT_POOL_RECOVER_LOCK);
        boolean locked = redisOperator.setIfAbsent(lockKey, "1", Duration.ofSeconds(55));
        if (!locked) {
            return;
        }
        try {
            aiAccountPoolService.recoverDisabledAccounts();
        } catch (Exception e) {
            log.error("账号池自动恢复异常: {}", e.getMessage(), e);
        }
    }
}
