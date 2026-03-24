package com.cybzacg.blogbackend.enums.storage;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文件上传模式枚举
 *
 * @author system
 * @since 2025/12/28
 */
public enum UploadModeEnum {

    /**
     * 秒传
     */
    QUICK_UPLOAD(1, "秒传"),

    /**
     * 分片上传
     */
    CHUNKED_UPLOAD(2, "分片上传"),

    /**
     * 全量上传
     */
    FULL_UPLOAD(3, "全量上传");

    private final Integer value;
    private final String label;

    UploadModeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return this.value;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 代码
     * @return 枚举
     */
    public static UploadModeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UploadModeEnum uploadModeEnum : values()) {
            if (uploadModeEnum.value.equals(code)) {
                return uploadModeEnum;
            }
        }
        return null;
    }
}

