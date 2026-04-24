package com.cybzacg.blogbackend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine 本地缓存配置。<p>仅在 spring.cache.type=caffeine 时生效，根据配置规格字符串初始化 CaffeineCacheManager。</p>
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(
        prefix = "spring.cache",
        name = "type",
        havingValue = "caffeine"
)
public class CaffeineConfig {

    @Value("${spring.cache.caffeine.spec}")
    private String caffeineSpec;

    /**
     * 缓存管理器
     *
     * @return CacheManager 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeineBuilder = Caffeine.from(caffeineSpec);
        caffeineCacheManager.setCaffeine(caffeineBuilder);
        return caffeineCacheManager;
    }
}
