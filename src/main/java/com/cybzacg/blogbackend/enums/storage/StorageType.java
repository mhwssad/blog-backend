package com.cybzacg.blogbackend.enums.storage;

import lombok.AllArgsConstructor;

/**
 * 存储类型枚举
 *
 * @author System
 */

@AllArgsConstructor
public enum StorageType {

    /**
     * 阿里云 OSS
     */
    OSS("oss", "阿里云 OSS 对象存储"),

    /**
     * 腾讯云 COS
     */
    COS("cos", "腾讯云 COS 对象存储"),

    /**
     * 本地存储
     */
    LOCAL("local", "本地文件存储"),

    /**
     * MinIO 对象存储
     */
    MINIO("minio", "MinIO 对象存储");

    /**
     * 存储类型代码
     */
    private final String code;

    /**
     * 存储类型描述
     */
    private final String description;

    public static StorageType fromCode(String storageTypeCode) {
        for (StorageType storageType : StorageType.values()) {
            if (storageTypeCode.equalsIgnoreCase(storageType.code)) {
                return storageType;
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

