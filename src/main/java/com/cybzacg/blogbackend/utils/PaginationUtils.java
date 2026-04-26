package com.cybzacg.blogbackend.utils;

/**
 * 分页参数标准化工具类。
 *
 * <p>统一收敛分页参数的空安全处理和边界约束逻辑。
 */
public final class PaginationUtils {

    private PaginationUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 标准化分页页码。
     *
     * @param current 原始页码，null 或小于 1 时返回 1
     * @return 标准化后的页码
     */
    public static long normalizeCurrent(Long current) {
        return current == null || current < 1L ? 1L : current;
    }

    /**
     * 标准化分页大小。
     *
     * @param size          原始分页大小
     * @param defaultValue  默认值
     * @param maxValue      上限值
     * @return 标准化后的分页大小，范围 [defaultValue, maxValue]
     */
    public static long normalizeSize(Long size, long defaultValue, long maxValue) {
        long normalized = size == null || size < 1L ? defaultValue : size;
        return Math.min(normalized, maxValue);
    }
}
