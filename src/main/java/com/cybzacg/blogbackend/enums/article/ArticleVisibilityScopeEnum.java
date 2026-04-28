package com.cybzacg.blogbackend.enums.article;

import lombok.Getter;

/**
 * 文章可见范围枚举。
 */
@Getter
public enum ArticleVisibilityScopeEnum {
    PUBLIC(0, "公开"),
    SELF_ONLY(1, "仅自己可见"),
    WHITELIST(2, "白名单可见"),
    LOGIN_REQUIRED(3, "登录可见");

    private final Integer value;
    private final String label;

    ArticleVisibilityScopeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static ArticleVisibilityScopeEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ArticleVisibilityScopeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(Integer value) {
        return fromValue(value) != null;
    }

    public static String resolveLabel(Integer value) {
        ArticleVisibilityScopeEnum scope = fromValue(value);
        return scope == null ? null : scope.getLabel();
    }
}
