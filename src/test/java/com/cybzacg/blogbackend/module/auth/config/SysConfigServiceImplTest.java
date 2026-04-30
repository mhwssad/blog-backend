package com.cybzacg.blogbackend.module.auth.config;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.module.auth.config.repository.SysConfigRepository;
import com.cybzacg.blogbackend.module.auth.config.service.impl.SysConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private SysConfigRepository sysConfigRepository;

    private SysConfigServiceImpl sysConfigService;

    @BeforeEach
    void setUp() {
        sysConfigService = new SysConfigServiceImpl(sysConfigRepository, redisOperator);
    }

    @Test
    void getValueByKeyShouldReturnNullWhenKeyBlank() {
        assertNull(sysConfigService.getValueByKey(" "));
        verify(redisOperator, never()).get(anyString());
        verify(sysConfigRepository, never()).findByConfigKey(anyString());
    }

    @Test
    void getValueByKeyShouldReturnCachedValueWhenCacheHit() {
        String cacheKey = cacheKey("site.title");
        when(redisOperator.get(cacheKey)).thenReturn("Blog");

        String result = sysConfigService.getValueByKey(" site.title ");

        assertEquals("Blog", result);
        verify(redisOperator).get(cacheKey);
        verify(sysConfigRepository, never()).findByConfigKey("site.title");
    }

    @Test
    void getValueByKeyShouldLoadFromRepositoryAndWriteCacheWhenCacheMiss() {
        String cacheKey = cacheKey("site.title");
        SysConfig config = config("site.title", "Blog");
        when(redisOperator.get(cacheKey)).thenReturn(null);
        when(sysConfigRepository.findByConfigKey("site.title")).thenReturn(config);

        String result = sysConfigService.getValueByKey(" site.title ");

        assertEquals("Blog", result);
        verify(redisOperator).set(cacheKey, "Blog");
    }

    @Test
    void getValueByKeyShouldReturnNullWhenConfigNotFound() {
        String cacheKey = cacheKey("site.title");
        when(redisOperator.get(cacheKey)).thenReturn(null);
        when(sysConfigRepository.findByConfigKey("site.title")).thenReturn(null);

        assertNull(sysConfigService.getValueByKey("site.title"));
        verify(redisOperator, never()).set(eq(cacheKey), isNull());
    }

    @Test
    void getValueOrDefaultShouldReturnDefaultWhenConfigMissing() {
        String cacheKey = cacheKey("missing.key");
        when(redisOperator.get(cacheKey)).thenReturn(null);
        when(sysConfigRepository.findByConfigKey("missing.key")).thenReturn(null);

        String result = sysConfigService.getValueOrDefault("missing.key", "default");

        assertEquals("default", result);
    }

    @Test
    void evictConfigCacheShouldDeleteNormalizedCacheKey() {
        sysConfigService.evictConfigCache(" site.title ");

        verify(redisOperator).delete(cacheKey("site.title"));
    }

    @Test
    void evictConfigCacheShouldSkipWhenKeyBlank() {
        sysConfigService.evictConfigCache(" ");
        verify(redisOperator, never()).delete(anyString());
    }

    private SysConfig config(String key, String value) {
        SysConfig config = new SysConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }

    private String cacheKey(String key) {
        return RedisKeyUtils.build(ConfigConstants.CACHE_KEY_PREFIX, key);
    }
}