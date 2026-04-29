package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 数据读取范围枚举。
 *
 * <p>定义 AI 渠道可以读取的用户数据范围。
 * {@code PRIVATE_CHAT} 为高风险范围，启用时必须记录审计日志。
 */
@Getter
@AllArgsConstructor
public enum AiDataScopeEnum {

    NONE("none", "无数据访问"),
    PUBLIC_ARTICLES("public_articles", "公开文章"),
    OWN_ARTICLES("own_articles", "用户自己的文章"),
    PROFILE("profile", "用户资料"),
    PUBLIC_CHAT("public_chat", "公开聊天"),
    PRIVATE_CHAT("private_chat", "私聊内容（高风险）");

    private final String code;
    private final String description;

    /**
     * 根据 code 获取枚举值。
     */
    public static AiDataScopeEnum fromCode(String code) {
        for (AiDataScopeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断是否为高风险范围。
     */
    public static boolean isHighRisk(String code) {
        return PRIVATE_CHAT.code.equals(code);
    }
}
