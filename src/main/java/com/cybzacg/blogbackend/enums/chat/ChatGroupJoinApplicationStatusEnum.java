package com.cybzacg.blogbackend.enums.chat;

import lombok.Getter;

/**
 * 群聊入群申请状态枚举。
 */
@Getter
public enum ChatGroupJoinApplicationStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    CANCELED(3, "已取消");

    private final Integer value;
    private final String label;

    ChatGroupJoinApplicationStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static ChatGroupJoinApplicationStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ChatGroupJoinApplicationStatusEnum item : values()) {
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
