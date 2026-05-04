package com.cybzacg.blogbackend.enums.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 禁言记录状态枚举。
 */
@Getter
@AllArgsConstructor
public enum ChatMuteRecordStatusEnum {

    RELEASED(0, "已解除"),
    ACTIVE(1, "生效中");

    private final Integer value;
    private final String label;

    public static ChatMuteRecordStatusEnum fromValue(Integer value) {
        for (ChatMuteRecordStatusEnum e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }
}
