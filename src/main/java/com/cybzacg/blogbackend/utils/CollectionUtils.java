package com.cybzacg.blogbackend.utils;

/**
 * 集合与数值工具类。
 *
 * <p>统一收敛项目中常见的空安全默认值处理逻辑。
 */
public final class CollectionUtils {

    private CollectionUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 空安全整数默认值。
     *
     * @param value        原值
     * @param defaultValue 默认值
     * @return value 为 null 时返回 defaultValue，否则返回 value
     */
    public static Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 空安全整数默认值，固定返回 0。
     */
    public static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 空安全整数默认值。
     *
     * @param value        原值
     * @param defaultValue 默认值
     * @return value 为 null 时返回 defaultValue，否则返回 value
     */
    public static Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 空安全长整数默认值，固定返回 0L。
     */
    public static long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
