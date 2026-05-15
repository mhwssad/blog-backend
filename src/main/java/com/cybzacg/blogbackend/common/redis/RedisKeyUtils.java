package com.cybzacg.blogbackend.common.redis;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.utils.StrUtils;

import java.util.StringJoiner;

/**
 * Redis Key 拼装工具。<p>按冒号分隔符将多个片段拼接为符合项目约定的 Key，自动忽略 {@code null} 和空白片段。
 */
public final class RedisKeyUtils {
    private RedisKeyUtils() {
    }

    /**
     * 按冒号拼装 Redis Key，自动忽略 null 和空白片段；没有有效片段时拒绝生成空 Key。
     */
    public static String build(Object... parts) {
        StringJoiner joiner = new StringJoiner(RedisConstants.KEY_SEPARATOR);
        if (parts != null) {
            for (Object part : parts) {
                if (part == null) {
                    continue;
                }
                String value = StrUtils.trim(String.valueOf(part));
                if (!value.isEmpty()) {
                    joiner.add(value);
                }
            }
        }
        String key = joiner.toString();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Redis Key 至少需要一个有效片段");
        }
        return key;
    }
}
