package com.cybzacg.blogbackend.common.storage;

import com.cybzacg.blogbackend.enums.storage.StorageHealthStatus;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储节点健康信息 DTO。<p>承载单个存储节点的探活结果，包括状态、连续成功/失败次数和最近一次错误信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageHealthInfo {

    /**
     * 存储节点标识
     */
    private String key;

    /**
     * 存储类型
     */
    private StorageType storageType;

    /**
     * 健康状态
     */
    private StorageHealthStatus status;

    /**
     * 最后检查时间
     */
    private LocalDateTime lastCheckTime;

    /**
     * 连续失败次数
     */
    private Integer failureCount;

    /**
     * 连续成功次数
     */
    private Integer successCount;

    /**
     * 错误信息（可选）
     */
    private String errorMessage;
}

