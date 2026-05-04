package com.cybzacg.blogbackend.enums.file;

import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 文件业务引用类型。
 */
@Getter
public enum FileReferenceTypeEnum {
    AVATAR("avatar", "用户头像"),
    CHAT_MESSAGE("chat_message", "聊天消息文件"),
    ARTICLE_ATTACHMENT("article_attachment", "文章附件"),
    TEMP("temp", "临时文件");

    private final String value;
    private final String label;

    FileReferenceTypeEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static FileReferenceTypeEnum fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        for (FileReferenceTypeEnum item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    public static String normalize(String value) {
        FileReferenceTypeEnum item = fromValue(value);
        return item == null ? TEMP.getValue() : item.getValue();
    }

    public static boolean contains(String value) {
        return fromValue(value) != null;
    }
}
