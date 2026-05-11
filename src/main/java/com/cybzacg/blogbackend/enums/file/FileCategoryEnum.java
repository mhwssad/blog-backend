package com.cybzacg.blogbackend.enums.file;

import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.Getter;

/**
 * 文件分类枚举。
 */
@Getter
public enum FileCategoryEnum {
    AVATAR("avatar", "头像"),
    ATTACHMENT("attachment", "附件"),
    COMMENT("comment", "评论图片"),
    CHAT_ATTACHMENT("chat_attachment", "聊天附件"),
    TEMP("temp", "临时文件");

    private final String value;
    private final String label;

    FileCategoryEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static FileCategoryEnum fromValue(String value) {
        if (!StrUtils.hasText(value)) {
            return null;
        }
        for (FileCategoryEnum item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    public static String normalize(String value) {
        FileCategoryEnum item = fromValue(value);
        return item == null ? TEMP.getValue() : item.getValue();
    }

    public static boolean contains(String value) {
        return fromValue(value) != null;
    }
}
