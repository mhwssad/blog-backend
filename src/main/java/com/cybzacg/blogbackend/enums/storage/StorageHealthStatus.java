package com.cybzacg.blogbackend.enums.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储健康状态枚举
 */
@Getter
@AllArgsConstructor
public enum StorageHealthStatus  {

    /**
     * 健康
     */
    HEALTHY("healthy", "健康"),

    /**
     * 不健康
     */
    UNHEALTHY("unhealthy", "不健康"),

    /**
     * 未知
     */
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String description;

    /**
     * 根据代码获取枚举实例
     *
     * @param code 状态代码
     * @return 枚举实例，未找到返回null
     */
    public static StorageHealthStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (StorageHealthStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }

    public String getValue() {
        return code;
    }

    public String getLabel() {
        return description;
    }
}

