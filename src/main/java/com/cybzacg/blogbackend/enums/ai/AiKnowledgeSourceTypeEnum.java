package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 知识源类型枚举。
 *
 * <p>定义可入库的知识源类型。
 * 私聊内容、私密文章、白名单文章、已删除/隐藏内容不允许入库。
 */
@Getter
@AllArgsConstructor
public enum AiKnowledgeSourceTypeEnum {

    PUBLIC_ARTICLE("public_article", "公开文章"),
    AUTHOR_PROFILE("author_profile", "作者公开资料"),
    FORUM_POST("forum_post", "论坛帖子"),
    ADMIN_ENTRY("admin_entry", "管理员维护知识条目");

    private final String code;
    private final String description;

    /**
     * 根据 code 获取枚举值。
     */
    public static AiKnowledgeSourceTypeEnum fromCode(String code) {
        for (AiKnowledgeSourceTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断 code 是否为有效枚举值。
     */
    public static boolean contains(String code) {
        return fromCode(code) != null;
    }
}
