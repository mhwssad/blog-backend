package com.cybzacg.blogbackend.common.redis;

import com.cybzacg.blogbackend.common.constant.RedisConstants;

import java.util.StringJoiner;

/**
 * Redis Key 工具类
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
