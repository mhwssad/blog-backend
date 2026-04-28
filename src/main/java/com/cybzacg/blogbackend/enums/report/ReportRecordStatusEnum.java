package com.cybzacg.blogbackend.enums.report;

import lombok.Getter;

/**
 * 举报单状态枚举。
 */
@Getter
public enum ReportRecordStatusEnum {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    HANDLED(2, "已处理"),
    REJECTED(3, "已驳回");

    private final Integer value;
    private final String label;

    ReportRecordStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static ReportRecordStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ReportRecordStatusEnum item : values()) {
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
