package com.cybzacg.blogbackend.common.storage.impl;


import com.cybzacg.blogbackend.common.storage.StorageHealthCheckService;
import com.cybzacg.blogbackend.common.storage.StorageHealthInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储健康检查指示器
 * 集成到 Spring Boot Actuator 健康检查中
 */
@Slf4j
public class StorageHealthIndicator implements HealthIndicator {

    private final StorageHealthCheckService storageHealthCheckService;

    public StorageHealthIndicator(StorageHealthCheckService storageHealthCheckService) {
        this.storageHealthCheckService = storageHealthCheckService;
    }

    @Override
    public Health health() {
        try {
            // 获取所有存储节点的健康状态
            List<StorageHealthInfo> healthInfoList = storageHealthCheckService.getAllHealthStatus();

            // 构建健康检查详情
            Map<String, Object> details = new HashMap<>();
            int totalStorages = healthInfoList.size();
            int healthyStorages = 0;
            int unhealthyStorages = 0;
            int unknownStorages = 0;

            // 添加每个存储节点的详细信息
            for (StorageHealthInfo healthInfo : healthInfoList) {
                Map<String, Object> storageDetails = new HashMap<>();
                storageDetails.put("status", healthInfo.getStatus().name());
                storageDetails.put("storageType", healthInfo.getStorageType().name());
                storageDetails.put("lastCheckTime", healthInfo.getLastCheckTime());
                storageDetails.put("successCount", healthInfo.getSuccessCount());
                storageDetails.put("failureCount", healthInfo.getFailureCount());

                if (healthInfo.getErrorMessage() != null) {
                    storageDetails.put("errorMessage", healthInfo.getErrorMessage());
                }

                details.put(healthInfo.getKey(), storageDetails);

                // 统计各状态数量
                switch (healthInfo.getStatus()) {
                    case HEALTHY:
                        healthyStorages++;
                        break;
                    case UNHEALTHY:
                        unhealthyStorages++;
                        break;
                    case UNKNOWN:
                        unknownStorages++;
                        break;
                }
            }

            // 添加汇总信息
            details.put("total", totalStorages);
            details.put("healthy", healthyStorages);
            details.put("unhealthy", unhealthyStorages);
            details.put("unknown", unknownStorages);

            // 根据健康状态返回相应的 Health 对象
            if (unhealthyStorages > 0) {
                // 有不健康的存储节点
                return Health.down()
                        .withDetail("storage", details)
                        .withDetail("summary", String.format("%d/%d 存储节点不健康", unhealthyStorages, totalStorages))
                        .build();
            } else if (unknownStorages > 0) {
                // 有未知状态的存储节点
                return Health.unknown()
                        .withDetail("storage", details)
                        .withDetail("summary", String.format("%d/%d 存储节点状态未知", unknownStorages, totalStorages))
                        .build();
            } else {
                // 所有存储节点都健康
                return Health.up()
                        .withDetail("storage", details)
                        .withDetail("summary", String.format("所有 %d 个存储节点都健康", totalStorages))
                        .build();
            }
        } catch (Exception e) {
            log.error("存储健康检查失败", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}

