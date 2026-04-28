package com.cybzacg.blogbackend.enums.report;

import lombok.Getter;

/**
 * 举报处理动作枚举。
 */
@Getter
public enum ReportActionTypeEnum {
    CLAIM("claim", "接单处理"),
    APPROVE("approve", "处理通过"),
    REJECT("reject", "驳回举报"),
    CLOSE("close", "关闭流程"),
    REASSIGN("reassign", "转派处理");

    private final String code;
    private final String label;

    ReportActionTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ReportActionTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ReportActionTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(String code) {
        return fromCode(code) != null;
    }
}
