package com.cybzacg.blogbackend.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 缓存管理器配置。<p>仅在 spring.cache.type=redis 时生效，自定义缓存序列化策略为 String/JSON，并支持通过配置调整 TTL、空值缓存和键前缀。</p>
 */
@EnableCaching
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(
        prefix = "spring.cache",    // 配置前缀
        name = "type",        // 配置名称（完整配置：spring.cache.type）
        havingValue = "redis"    // 预期的配置值
) // spring.cache.type = redis 时才会自动装配
public class RedisCacheConfig {

    /**
     * 创建自定义 RedisCacheManager，使用 String/JSON 序列化替代默认的 JDK 序列化。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @param cacheProperties        Spring Boot 缓存配置属性
     * @return RedisCacheManager 实例
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory, CacheProperties cacheProperties) {
        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
                .cacheDefaults(redisCacheConfiguration(cacheProperties))
                .build();
    }

    /**
     * 构建 Redis 缓存配置，覆盖默认的双冒号键前缀为单冒号。
     *
     * @param cacheProperties Spring Boot 缓存配置属性
     * @return RedisCacheConfiguration 实例
     */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()));
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        CacheProperties.Redis redisProperties = cacheProperties.getRedis();

        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        // 覆盖默认key双冒号  CacheKeyPrefix#prefixed
        config = config.computePrefixWith(name -> name + ":");
        return config;
    }

}

