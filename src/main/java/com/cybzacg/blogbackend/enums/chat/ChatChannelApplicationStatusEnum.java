package com.cybzacg.blogbackend.enums.chat;

import lombok.Getter;

/**
 * 频道创建申请状态枚举。
 */
@Getter
public enum ChatChannelApplicationStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    NEED_MORE_INFO(3, "待补充");

    private final Integer value;
    private final String label;

    ChatChannelApplicationStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static ChatChannelApplicationStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ChatChannelApplicationStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(Integer value) {
        return fromValue(value) != null;
    }
}
