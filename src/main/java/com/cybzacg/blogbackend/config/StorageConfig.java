package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.common.storage.StorageHealthCheckService;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.common.storage.StorageServiceFactoryManager;
import com.cybzacg.blogbackend.common.storage.impl.StorageHealthCheckServiceImpl;
import com.cybzacg.blogbackend.common.storage.impl.StorageHealthIndicator;
import com.cybzacg.blogbackend.common.storage.impl.StorageManagerImpl;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageManagerProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 存储配置类
 * 配置多种存储类型（OSS、COS、Local、MinIO），支持多存储节点
 * 统一使用 StorageManager 作为存储服务的入口，提供健康检查、故障转移、负载均衡等功能
 *
 * @author System
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StorageConfig {
    private final StorageProperties storageProperties;
    private final FileUploadProperties fileUploadProperties;
    private final StorageServiceFactoryManager factoryManager;

    /**
     * 创建多存储服务 Map
     * 支持通过 key 获取不同的存储服务实例
     * 使用工厂模式创建存储服务实例，便于扩展新的存储类型
     *
     * @return Map<String, StorageService> 存储服务映射
     */
    @Bean
    public Map<String, StorageService> storageServiceMap() {
        // 校验存储配置是否为空
        if (storageProperties.getStorage() == null || storageProperties.getStorage().isEmpty()) {
            log.error("存储配置为空，请检查 application.yml 中的 storage.storage 配置");
            throw new IllegalStateException("存储配置不能为空，请配置至少一个存储节点");
        }

        Map<String, StorageService> serviceMap = new HashMap<>();

        for (StorageProperties.Storage config : storageProperties.getStorage()) {
            String key = config.getKey();
            String storageType = config.getType();

            // 使用工厂管理器创建存储服务实例
            try {
                StorageService storageService = factoryManager.createStorageService(
                        storageType,
                        config,
                        fileUploadProperties
                );

                serviceMap.put(key, storageService);

                // 根据不同的存储类型记录不同的日志信息
                switch (Objects.requireNonNull(StorageType.fromCode(storageType))) {
                    case OSS:
                        log.info("初始化 OSS 存储服务: key={}, endpoint={}", key, config.getEndpoint());
                        break;
                    case COS:
                        log.info("初始化 COS 存储服务: key={}", key);
                        break;
                    case LOCAL:
                        log.info("初始化本地存储服务: key={}, path={}", key, config.getBucketName());
                        break;
                    case MINIO:
                        log.info("初始化 MinIO 存储服务: key={}, endpoint={}", key, config.getEndpoint());
                        break;
                    default:
                        log.info("初始化存储服务: key={}, type={}", key, storageType);
                }
            } catch (IllegalArgumentException e) {
                log.error("创建存储服务失败: key={}, type={}, error={}", key, storageType, e.getMessage());
                throw e;
            }
        }

        if (serviceMap.isEmpty()) {
            log.error("未成功初始化任何存储服务，请检查配置");
            throw new IllegalStateException("未成功初始化任何存储服务");
        }

        log.info("初始化多存储服务完成，共 {} 个节点", serviceMap.size());
        return serviceMap;
    }

    /**
     * 创建存储健康检查服务 Bean。
     *
     * <p>线程池统一复用 {@link ThreadPoolConfig} 中的定时任务线程池和默认异步线程池，
     * 避免存储模块单独维护一套执行器配置。
     *
     * @param storageServiceMap        所有存储服务Map
     * @param storageProperties        存储配置
     * @param storageManagerProperties 管理器配置
     * @param scheduledTaskExecutor    定时任务执行器
     * @param asyncTaskExecutor        默认异步线程池（用于并发执行健康检查）
     * @return StorageHealthCheckService 实例
     */
    @Bean
    public StorageHealthCheckService storageHealthCheckService(
            Map<String, StorageService> storageServiceMap,
            StorageProperties storageProperties,
            StorageManagerProperties storageManagerProperties,
            @Qualifier("scheduledTaskExecutor") ScheduledExecutorService scheduledTaskExecutor,
            @Qualifier("asyncTaskExecutor") Executor asyncTaskExecutor) {
        log.info("初始化存储健康检查服务，监控节点数: {}, 检查间隔: {} 秒",
                storageServiceMap.size(),
                storageManagerProperties.getHealthCheckInterval());
        return new StorageHealthCheckServiceImpl(
                storageServiceMap,
                storageProperties,
                storageManagerProperties,
                scheduledTaskExecutor,
                asyncTaskExecutor);
    }

    /**
     * 创建存储管理器 Bean
     * 作为默认的StorageService实现，提供统一入口
     * 支持@Primary注解，在注入StorageService时优先使用此Bean
     * 所有存储操作应统一使用StorageManager，它提供了：
     * 1. 存储服务的统一管理
     * 2. 健康检查和故障转移
     * 3. 负载均衡策略
     * 4. 手动切换存储节点功能
     *
     * @param storageServiceMap        所有存储服务Map
     * @param storageProperties        存储配置
     * @param healthCheckService       健康检查服务
     * @param storageManagerProperties 管理器配置
     * @return StorageManager 实例
     */
    @Bean
    @Primary
    public StorageManager storageManager(
            Map<String, StorageService> storageServiceMap,
            StorageProperties storageProperties,
            StorageHealthCheckService healthCheckService,
            StorageManagerProperties storageManagerProperties) {
        log.info("初始化存储管理器，默认存储类型: {}, 选择策略: {}, 管理节点数: {}",
                storageProperties.getStorageType(),
                storageManagerProperties.getStrategyEnum(),
                storageServiceMap.size());
        return new StorageManagerImpl(
                storageServiceMap,
                storageProperties,
                healthCheckService,
                storageManagerProperties);
    }

    /**
     * 创建存储健康检查指示器 Bean
     * 集成到 Spring Boot Actuator 健康检查中
     * Bean 名称指定为 "storage"，这样 Actuator 会将其识别为 storage 健康检查
     *
     * @param storageHealthCheckService 存储健康检查服务
     * @return StorageHealthIndicator 实例
     */
    @Bean("storage")
    public StorageHealthIndicator storageHealthIndicator(StorageHealthCheckService storageHealthCheckService) {
        log.info("初始化存储健康检查指示器");
        return new StorageHealthIndicator(storageHealthCheckService);
    }
}
