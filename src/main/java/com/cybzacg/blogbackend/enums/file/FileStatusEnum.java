package com.cybzacg.blogbackend.enums.file;

import lombok.Getter;

/**
 * 文件状态枚举。
 */
@Getter
public enum FileStatusEnum {
    DELETED(0, "已删除"),
    NORMAL(1, "正常"),
    PHYSICAL_DELETE_PENDING(2, "待物理删除"),
    REVIEWING(3, "审核中"),
    VIOLATION(4, "违规下架");

    private final Integer value;
    private final String label;

    FileStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static FileStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (FileStatusEnum item : values()) {
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
