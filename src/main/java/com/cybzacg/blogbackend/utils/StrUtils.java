package com.cybzacg.blogbackend.utils;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 字符串工具类。
 *
 * <p>统一收敛项目中常见的判空、裁剪、大小写转换和默认值处理逻辑。
 */
public final class StrUtils {

    private StrUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 判断字符串是否包含可见文本。
     */
    public static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    /**
     * 对字符串做空安全裁剪。
     */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 仅在字符串包含可见文本时裁剪，否则保留原值。
     */
    public static String normalize(String value) {
        return hasText(value) ? value.trim() : value;
    }

    /**
     * 将空白字符串视为 null，其余情况返回裁剪后的结果。
     */
    public static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 对字符串裁剪后转为小写，适用于邮箱等大小写不敏感字段。
     */
    public static String trimToLowerCase(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 字符串为空白时返回默认值，否则返回裁剪后的结果。
     */
    public static String trimToDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * 判断字符串是否为空白（null、空串或全空白字符）。
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 判断字符串是否非空白。
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /**
     * 判断字符串是否为空（null 或空串），不裁剪。
     */
    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * 判断字符串是否非空。
     */
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    /**
     * 空白时返回默认值，否则返回原值（不裁剪）。
     */
    public static String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * 空安全字符串包含检查。
     *
     * @return source 为空白或不含 keyword 时返回 false
     */
    public static boolean contains(String source, String keyword) {
        return StringUtils.hasText(source) && source.contains(keyword);
    }

    /**
     * 将 null 转为空串，非 null 原样返回。
     */
    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
