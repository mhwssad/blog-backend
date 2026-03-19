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
}
