package com.cybzacg.blogbackend.common.redis;

import com.cybzacg.blogbackend.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作封装。<p>对 {@link RedisTemplate} 做二次封装，提供 Key、String、Hash、List、Set、ZSet
 * 等常用数据结构的快捷方法，同时将 {@code null} 返回值统一转为 Java 默认值，
 * 降低上层业务调用的空指针风险。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOperator {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 判断 Key 是否存在。
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置 Key 的过期时间。{@code timeout} 为 {@code null} 时直接返回 {@code false}。
     */
    public boolean expire(String key, Duration timeout) {
        return timeout != null && Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
    }

    /**
     * 设置 Key 的过期时间。{@code timeout <= 0} 或 {@code unit} 为 {@code null} 时直接返回 {@code false}。
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return timeout > 0 && unit != null && Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 移除 Key 的过期时间，使其永久有效。
     */
    public boolean persist(String key) {
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    /**
     * 获取 Key 的剩余过期时间。Key 不存在时返回 -1。
     */
    public long getExpire(String key, TimeUnit unit) {
        Long expire = redisTemplate.getExpire(key, unit);
        return expire != null ? expire : -1L;
    }

    /**
     * 删除单个 Key。
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除 Key，返回实际删除数量。
     */
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted != null ? deleted : 0L;
    }

    /**
     * 设置 String 值，不设过期时间。
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public void set(String key, Object value) {
        log.warn("Redis Key 写入未设置 TTL: key={}", key);
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置 String 值并指定过期时间。{@code timeout} 无效时退化为永久有效。
     */
    public void set(String key, Object value, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            set(key, value);
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 仅当 Key 不存在时设置值（SET NX）。
     */
    public boolean setIfAbsent(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value));
    }

    /**
     * 仅当 Key 不存在时设置值并指定过期时间。{@code timeout} 无效时退化为不带 TTL 的 SET NX。
     */
    public boolean setIfAbsent(String key, Object value, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return setIfAbsent(key, value);
        }
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
    }

    /**
     * 仅当 Key 已存在时设置值（SET XX）。
     */
    public boolean setIfPresent(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfPresent(key, value));
    }

    /**
     * 获取 String 值的原始 Object。
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取 String 值并自动转换为指定类型。
     */
    public <T> T get(String key, Class<T> clazz) {
        return convertValue(get(key), clazz);
    }

    /**
     * 获取 String 值并通过 {@link TypeReference} 自动转换为复杂泛型类型。
     */
    public <T> T get(String key, TypeReference<T> typeReference) {
        return convertValue(get(key), typeReference);
    }

    /**
     * 批量获取多个 Key 的值。Key 不存在时对应位置为 {@code null}。
     */
    public List<Object> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        return values != null ? values : Collections.emptyList();
    }

    /**
     * 自增 1。
     */
    public long increment(String key) {
        return increment(key, 1L);
    }

    /**
     * 自增指定步长。
     */
    public long increment(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value != null ? value : 0L;
    }

    /**
     * 自增指定浮点步长。
     */
    public double increment(String key, double delta) {
        Double value = redisTemplate.opsForValue().increment(key, delta);
        return value != null ? value : 0D;
    }

    /**
     * 自减 1。
     */
    public long decrement(String key) {
        return decrement(key, 1L);
    }

    /**
     * 自减指定步长。
     */
    public long decrement(String key, long delta) {
        Long value = redisTemplate.opsForValue().decrement(key, delta);
        return value != null ? value : 0L;
    }

    /**
     * 向 Hash 中写入单个字段。
     */
    public void hashPut(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 向 Hash 中批量写入字段。{@code entries} 为空时直接跳过。
     */
    public void hashPutAll(String key, Map<String, ?> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, entries);
    }

    /**
     * 获取 Hash 中指定字段的原始值。
     */
    public Object hashGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取 Hash 中指定字段并自动转换为指定类型。
     */
    public <T> T hashGet(String key, String hashKey, Class<T> clazz) {
        return convertValue(hashGet(key, hashKey), clazz);
    }

    /**
     * 判断 Hash 中是否存在指定字段。
     */
    public boolean hashHasKey(String key, String hashKey) {
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, hashKey));
    }

    /**
     * 删除 Hash 中的一个或多个字段，返回实际删除数量。
     */
    public long hashDelete(String key, Object... hashKeys) {
        if (hashKeys == null || hashKeys.length == 0) {
            return 0L;
        }
        Long deleted = redisTemplate.opsForHash().delete(key, hashKeys);
        return deleted != null ? deleted : 0L;
    }

    /**
     * 获取 Hash 的全部字段和值。
     */
    public Map<Object, Object> hashEntries(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return entries != null ? entries : Collections.emptyMap();
    }

    /**
     * Hash 字段自增指定整数步长。
     */
    public long hashIncrement(String key, String hashKey, long delta) {
        Long value = redisTemplate.opsForHash().increment(key, hashKey, delta);
        return value != null ? value : 0L;
    }

    /**
     * Hash 字段自增指定浮点步长。
     */
    public double hashIncrement(String key, String hashKey, double delta) {
        Double value = redisTemplate.opsForHash().increment(key, hashKey, delta);
        return value != null ? value : 0D;
    }

    /**
     * 从 List 左端压入元素，返回压入后的列表长度。
     */
    public long leftPush(String key, Object value) {
        Long size = redisTemplate.opsForList().leftPush(key, value);
        return size != null ? size : 0L;
    }

    /**
     * 从 List 右端压入元素，返回压入后的列表长度。
     */
    public long rightPush(String key, Object value) {
        Long size = redisTemplate.opsForList().rightPush(key, value);
        return size != null ? size : 0L;
    }

    /**
     * 从 List 左端弹出元素。
     */
    public Object leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 从 List 右端弹出元素。
     */
    public Object rightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取 List 指定区间内的元素。
     */
    public List<Object> listRange(String key, long start, long end) {
        List<Object> values = redisTemplate.opsForList().range(key, start, end);
        return values != null ? values : Collections.emptyList();
    }

    /**
     * 获取 List 的长度。
     */
    public long listSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0L;
    }

    /**
     * 从 List 中移除指定个数的匹配元素。
     */
    public long listRemove(String key, long count, Object value) {
        Long removed = redisTemplate.opsForList().remove(key, count, value);
        return removed != null ? removed : 0L;
    }

    /**
     * 向 Set 中添加一个或多个元素，返回实际添加数量。
     */
    public long setAdd(String key, Object... values) {
        if (values == null || values.length == 0) {
            return 0L;
        }
        Long added = redisTemplate.opsForSet().add(key, values);
        return added != null ? added : 0L;
    }

    /**
     * 从 Set 中移除一个或多个元素，返回实际移除数量。
     */
    public long setRemove(String key, Object... values) {
        if (values == null || values.length == 0) {
            return 0L;
        }
        Long removed = redisTemplate.opsForSet().remove(key, values);
        return removed != null ? removed : 0L;
    }

    /**
     * 判断元素是否在 Set 中。
     */
    public boolean isMember(String key, Object value) {
        BoundSetOperations<String, Object> operations = redisTemplate.boundSetOps(key);
        return Boolean.TRUE.equals(operations.isMember(value));
    }

    /**
     * 获取 Set 的全部元素。
     */
    public Set<Object> members(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        return members != null ? members : Collections.emptySet();
    }

    /**
     * 获取 Set 的元素数量。
     */
    public long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }

    /**
     * 向 Sorted Set 中添加元素及分数。
     */
    public boolean zAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    /**
     * 批量向 Sorted Set 中添加元素。
     */
    public long zAdd(String key, Set<ZSetOperations.TypedTuple<Object>> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        Long added = redisTemplate.opsForZSet().add(key, values);
        return added != null ? added : 0L;
    }

    /**
     * 获取 Sorted Set 中指定元素的分数。
     */
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 按分数从小到大获取 Sorted Set 指定区间内的元素。
     */
    public Set<Object> zRange(String key, long start, long end) {
        Set<Object> values = redisTemplate.opsForZSet().range(key, start, end);
        return values != null ? values : Collections.emptySet();
    }

    /**
     * 按分数从大到小获取 Sorted Set 指定区间内的元素。
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        Set<Object> values = redisTemplate.opsForZSet().reverseRange(key, start, end);
        return values != null ? values : Collections.emptySet();
    }

    /**
     * 从 Sorted Set 中移除一个或多个元素。
     */
    public long zRemove(String key, Object... values) {
        if (values == null || values.length == 0) {
            return 0L;
        }
        Long removed = redisTemplate.opsForZSet().remove(key, values);
        return removed != null ? removed : 0L;
    }

    /**
     * 获取 Sorted Set 的元素数量。
     */
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
