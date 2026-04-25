package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.mapper.SysConfigMapper;
import com.cybzacg.blogbackend.module.auth.service.impl.SysConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private SysConfigMapper sysConfigMapper;

    private SysConfigServiceImpl sysConfigService;

    @BeforeEach
    void setUp() {
        sysConfigService = new SysConfigServiceImpl(redisOperator);
        ReflectionTestUtils.setField(sysConfigService, "baseMapper", sysConfigMapper);
    }

    @Test
    void getValueByKeyShouldReturnNullWhenKeyBlank() {
        assertNull(sysConfigService.getValueByKey(" "));
        verify(redisOperator, never()).get(org.mockito.ArgumentMatchers.anyString());
        verify(sysConfigMapper, never()).selectByConfigKey(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getValueByKeyShouldReturnCachedValueWhenCacheHit() {
        String cacheKey = cacheKey("site.title");
        when(redisOperator.get(cacheKey)).thenReturn("Blog");

        String result = sysConfigService.getValueByKey(" site.title ");

        assertEquals("Blog", result);
        verify(redisOperator).get(cacheKey);
        verify(sysConfigMapper, never()).selectByConfigKey("site.title");
    }

    @Test
    void getValueByKeyShouldLoadFromMapperAndWriteCacheWhenCacheMiss() {
        String cacheKey = cacheKey("site.title");
        SysConfig config = config("site.title", "Blog");
        when(redisOperator.get(cacheKey)).thenReturn(null);
        when(sysConfigMapper.selectByConfigKey("site.title")).thenReturn(config);

        String result = sysConfigService.getValueByKey(" site.title ");

        assertEquals("Blog", result);
        verify(redisOperator).set(cacheKey, "Blog");
    }

    @Test
    void getValueByKeyShouldReturnNullWhenConfigNotFound() {
        String cacheKey = cacheKey("site.title");
        when(redisOperator.get(cacheKey)).thenReturn(null);
        when(sysConfigMapper.selectByConfigKey("site.title")).thenReturn(null);

        assertNull(sysConfigService.getValueByKey("site.title"));
        verify(redisOperator, never()).set(cacheKey, null);
    }

    @Test
    void getValueOrDefaultShouldReturnDefaultWhenConfigMissing() {
        when(redisOperator.get(cacheKey("missing.key"))).thenReturn(null);
        when(sysConfigMapper.selectByConfigKey("missing.key")).thenReturn(null);

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
        verify(redisOperator, never()).delete(org.mockito.ArgumentMatchers.anyString());
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
