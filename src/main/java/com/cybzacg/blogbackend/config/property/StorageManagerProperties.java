package com.cybzacg.blogbackend.config.property;

import com.cybzacg.blogbackend.enums.storage.StorageStrategyEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 存储管理器配置属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage.manager")
public class StorageManagerProperties {

    /**
     * 存储策略（默认：DEFAULT）
     * 可选值：DEFAULT, FAILOVER, ROUND_ROBIN, RANDOM
     */
    private String strategy = "DEFAULT";

    /**
     * 是否启用健康检查（默认：true）
     */
    private Boolean enableHealthCheck = true;

    /**
     * 健康检查间隔（毫秒，默认：60000ms，即1分钟）
     */
    private Long healthCheckInterval = 60000L;

    /**
     * 健康检查超时时间（毫秒，默认：5000ms）
     */
    private Long healthCheckTimeout = 5000L;

    /**
     * 最大连续失败次数（默认：3次）
     * 超过此次数后标记为不健康
     */
    private Integer maxFailureCount = 3;

    /**
     * 恢复健康所需的最小成功次数（默认：2次）
     * 连续成功此次数后从不健康恢复为健康
     */
    private Integer minSuccessCount = 2;

    /**
     * 是否启用负载均衡（默认：false）
     * 仅在策略为ROUND_ROBIN或RANDOM时生效
     */
    private Boolean enableLoadBalancing = false;

    /**
     * 获取存储策略枚举
     *
     * @return 存储策略枚举，未配置返回DEFAULT
     */
    public StorageStrategyEnum getStrategyEnum() {
        StorageStrategyEnum strategyEnum = StorageStrategyEnum.fromCode(strategy);
        return strategyEnum != null ? strategyEnum : StorageStrategyEnum.DEFAULT;
    }
}

