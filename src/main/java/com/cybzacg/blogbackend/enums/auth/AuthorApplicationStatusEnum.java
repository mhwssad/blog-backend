package com.cybzacg.blogbackend.enums.auth;

import lombok.Getter;

/**
 * 作者申请状态枚举。
 */
@Getter
public enum AuthorApplicationStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    NEED_MORE_INFO(3, "待补充");

    private final Integer value;
    private final String label;

    AuthorApplicationStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static AuthorApplicationStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AuthorApplicationStatusEnum item : values()) {
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
        AuthorApplicationStatusEnum status = fromValue(value);
        return status == null ? null : status.getLabel();
    }
}
