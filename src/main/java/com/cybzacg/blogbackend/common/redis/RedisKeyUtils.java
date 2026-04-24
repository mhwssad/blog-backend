package com.cybzacg.blogbackend.common.redis;

import com.cybzacg.blogbackend.common.constant.RedisConstants;

import java.util.StringJoiner;

/**
 * Redis Key 拼装工具。<p>按冒号分隔符将多个片段拼接为符合项目约定的 Key，自动忽略 {@code null} 和空白片段。
 */
public final class RedisKeyUtils {
    private RedisKeyUtils() {
    }

    /**
     * 按冒号拼装 Redis Key，自动忽略 null 和空白片段。
     */
    public static String build(Object... parts) {
        StringJoiner joiner = new StringJoiner(RedisConstants.KEY_SEPARATOR);
        if (parts == null || parts.length == 0) {
            return "";
        }

        for (Object part : parts) {
            if (part == null) {
                continue;
            }
            String value = String.valueOf(part).trim();
            if (!value.isEmpty()) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }
}
