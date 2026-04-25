package com.cybzacg.blogbackend.enums.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储策略枚举
 * 定义存储选择策略
 */
@Getter
@AllArgsConstructor
public enum StorageStrategyEnum {

    /**
     * 默认策略，使用配置的默认存储节点
     */
    DEFAULT("default", "默认策略"),

    /**
     * 故障转移策略，按顺序尝试备用节点
     */
    FAILOVER("failover", "故障转移策略"),

    /**
     * 轮询负载均衡策略
     */
    ROUND_ROBIN("round_robin", "轮询负载均衡策略"),

    /**
     * 随机负载均衡策略
     */
    RANDOM("random", "随机负载均衡策略");

    private final String code;
    private final String description;

    /**
     * 根据代码获取枚举实例
     *
     * @param code 策略代码
     * @return 枚举实例，未找到返回null
     */
    public static StorageStrategyEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (StorageStrategyEnum strategy : values()) {
            if (strategy.code.equalsIgnoreCase(code)) {
                return strategy;
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

