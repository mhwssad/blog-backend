package com.cybzacg.blogbackend.common.redis;

import com.cybzacg.blogbackend.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作封装
 */
@Component
@RequiredArgsConstructor
public class RedisOperator {
    private final RedisTemplate<String, Object> redisTemplate;

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public boolean expire(String key, Duration timeout) {
        return timeout != null && Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return timeout > 0 && unit != null && Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    public boolean persist(String key) {
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    public long getExpire(String key, TimeUnit unit) {
        Long expire = redisTemplate.getExpire(key, unit);
        return expire != null ? expire : -1L;
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted != null ? deleted : 0L;
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            set(key, value);
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    public boolean setIfAbsent(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value));
    }

    public boolean setIfAbsent(String key, Object value, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return setIfAbsent(key, value);
        }
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
    }

    public boolean setIfPresent(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfPresent(key, value));
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        return convertValue(get(key), clazz);
    }

    public <T> T get(String key, TypeReference<T> typeReference) {
        return convertValue(get(key), typeReference);
    }

    public List<Object> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        return values != null ? values : Collections.emptyList();
    }

    public long increment(String key) {
        return increment(key, 1L);
    }

    public long increment(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value != null ? value : 0L;
    }

    public double increment(String key, double delta) {
        Double value = redisTemplate.opsForValue().increment(key, delta);
        return value != null ? value : 0D;
    }

    public long decrement(String key) {
        return decrement(key, 1L);
    }

    public long decrement(String key, long delta) {
        Long value = redisTemplate.opsForValue().decrement(key, delta);
        return value != null ? value : 0L;
    }

    public void hashPut(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public void hashPutAll(String key, Map<String, ?> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, entries);
    }

    public Object hashGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public <T> T hashGet(String key, String hashKey, Class<T> clazz) {
        return convertValue(hashGet(key, hashKey), clazz);
    }

    public boolean hashHasKey(String key, String hashKey) {
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, hashKey));
    }

    public long hashDelete(String key, Object... hashKeys) {
        if (hashKeys == null || hashKeys.length == 0) {
            return 0L;
        }
        Long deleted = redisTemplate.opsForHash().delete(key, hashKeys);
        return deleted != null ? deleted : 0L;
    }

    public Map<Object, Object> hashEntries(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return entries != null ? entries : Collections.emptyMap();
    }

    public long hashIncrement(String key, String hashKey, long delta) {
        Long value = redisTemplate.opsForHash().increment(key, hashKey, delta);
        return value != null ? value : 0L;
    }

    public double hashIncrement(String key, String hashKey, double delta) {
        Double value = redisTemplate.opsForHash().increment(key, hashKey, delta);
        return value != null ? value : 0D;
    }

    public long leftPush(String key, Object value) {
        Long size = redisTemplate.opsForList().leftPush(key, value);
        return size != null ? size : 0L;
    }

    public long rightPush(String key, Object value) {
        Long size = redisTemplate.opsForList().rightPush(key, value);
        return size != null ? size : 0L;
    }

    public Object leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public Object rightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    public List<Object> listRange(String key, long start, long end) {
        List<Object> values = redisTemplate.opsForList().range(key, start, end);
        return values != null ? values : Collections.emptyList();
    }

    public long listSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0L;
    }

    public long listRemove(String key, long count, Object value) {
        Long removed = redisTemplate.opsForList().remove(key, count, value);
        return removed != null ? removed : 0L;
    }

    public long setAdd(String key, Object... values) {
        if (values == null || values.length == 0) {
            return 0L;
        }
        Long added = redisTemplate.opsForSet().add(key, values);
        return added != null ? added : 0L;
    }

    public long setRemove(String key, Object... values) {
        if (values == null || values.length == 0) {
            return 0L;
        }
        Long removed = redisTemplate.opsForSet().remove(key, values);
        return removed != null ? removed : 0L;
    }

    public boolean isMember(String key, Object value) {
        BoundSetOperations<String, Object> operations = redisTemplate.boundSetOps(key);
        return Boolean.TRUE.equals(operations.isMember(value));
    }

    public Set<Object> members(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        return members != null ? members : Collections.emptySet();
    }

    public long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }

    public boolean zAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    public long zAdd(String key, Set<ZSetOperations.TypedTuple<Object>> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        Long added = redisTemplate.opsForZSet().add(key, values);
        return added != null ? added : 0L;
    }

    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public Set<Object> zRange(String key, long start, long end) {
        Set<Object> values = redisTemplate.opsForZSet().range(key, start, end);
        return values != null ? values : Collections.emptySet();
    }

    public Set<Object> zReverseRange(String key, long start, long end) {
        Set<Object> values = redisTemplate.opsForZSet().reverseRange(key, start, end);
        return values != null ? values : Collections.emptySet();
    }

    public long zRemove(String key, Object... values) {
        if (values == null || values.length == 0) {
            return 0L;
        }
        Long removed = redisTemplate.opsForZSet().remove(key, values);
        return removed != null ? removed : 0L;
    }

    public long zSize(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return size != null ? size : 0L;
    }

    private <T> T convertValue(Object value, Class<T> clazz) {
        if (value == null || clazz == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return JsonUtils.getObjectMapper().convertValue(value, clazz);
    }

    private <T> T convertValue(Object value, TypeReference<T> typeReference) {
        if (value == null || typeReference == null) {
            return null;
        }
        return JsonUtils.getObjectMapper().convertValue(value, typeReference);
    }
}
