package com.cybzacg.blogbackend.common.storage.impl;


import com.cybzacg.blogbackend.common.storage.StorageHealthCheckService;
import com.cybzacg.blogbackend.common.storage.StorageHealthInfo;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.StorageManagerProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.storage.StorageStrategyEnum;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 存储管理器实现类
 * 提供存储服务的统一入口，支持健康检查、故障转移和负载均衡
 */
@Slf4j
public class StorageManagerImpl implements StorageManager {
    private final Map<String, StorageService> storageServiceMap;
    private final StorageProperties storageProperties;
    private final StorageHealthCheckService healthCheckService;
    private final StorageManagerProperties managerProperties;

    /**
     * 轮询计数器
     */
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

    /**
     * 当前存储策略
     */
    private volatile StorageStrategyEnum currentStrategy;

    /**
     * 当前使用的存储节点标识（用于DEFAULT策略）
     */
    private volatile String currentStorageKey;

    public StorageManagerImpl(
            Map<String, StorageService> storageServiceMap,
            StorageProperties storageProperties,
            StorageHealthCheckService healthCheckService,
            StorageManagerProperties managerProperties) {
        this.storageServiceMap = storageServiceMap;
        this.storageProperties = storageProperties;
        this.healthCheckService = healthCheckService;
        this.managerProperties = managerProperties;
        this.currentStrategy = managerProperties.getStrategyEnum();

        // 初始化当前存储节点
        initializeCurrentStorage();

        log.info("存储管理器初始化完成，策略: {}, 当前存储节点: {}",
                currentStrategy, currentStorageKey);
    }

    /**
     * 初始化当前存储节点
     */
    private void initializeCurrentStorage() {
        // 如果是DEFAULT策略，使用配置的默认存储类型
        if (currentStrategy == StorageStrategyEnum.DEFAULT) {
            String defaultStorageType = storageProperties.getStorageType();

            // 从存储列表中找到第一个匹配默认类型的节点
            if (storageProperties.getStorage() != null) {
                for (StorageProperties.Storage storage : storageProperties.getStorage()) {
                    if (storage.getType().equalsIgnoreCase(defaultStorageType)) {
                        currentStorageKey = storage.getKey();
                        break;
                    }
                }
            }

            // 如果没找到，使用第一个节点
            if (currentStorageKey == null && storageProperties.getStorage() != null
                    && !storageProperties.getStorage().isEmpty()) {
                currentStorageKey = storageProperties.getStorage().get(0).getKey();
            }
        }
    }

    // ========== StorageService 接口实现 ==========

    @Override
    public String upload(InputStream inputStream, String objectName) {
        return executeWithFailover(service -> service.upload(inputStream, objectName));
    }

    @Override
    public String upload(InputStream inputStream, String objectName, String contentType) {
        return executeWithFailover(service -> service.upload(inputStream, objectName, contentType));
    }

    @Override
    public InputStream download(String objectName) {
        return executeWithFailover(new StorageOperation<InputStream>() {
            @Override
            public InputStream execute(StorageService service) {
                return service.download(objectName);
            }
        });
    }

    @Override
    public boolean delete(String objectName) {
        return executeWithFailover(service -> service.delete(objectName));
    }

    @Override
    public int deleteBatch(List<String> objectNames) {
        return executeWithFailover(service -> service.deleteBatch(objectNames));
    }

    @Override
    public boolean exists(String objectName) {
        return executeWithFailover(service -> service.exists(objectName));
    }

    @Override
    public String getUrl(String objectName) {
        return executeWithFailover(service -> service.getUrl(objectName));
    }

    @Override
    public StorageType getStorageType() {
        StorageService service = getStorageService();
        return service != null ? service.getStorageType() : null;
    }

    @Override
    public String uploadToTemp(InputStream inputStream, String objectName) {
        return executeWithFailover(service -> service.uploadToTemp(inputStream, objectName));
    }

    @Override
    public String uploadToTemp(InputStream inputStream, String objectName, String contentType) {
        return executeWithFailover(service -> service.uploadToTemp(inputStream, objectName, contentType));
    }

    @Override
    public boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName) {
        return executeWithFailover(service -> service.mergeFiles(sourceObjectNames, targetObjectName));
    }

    @Override
    public boolean deleteTempFiles(String uploadId) {
        return executeWithFailover(service -> service.deleteTempFiles(uploadId));
    }

    @Override
    public boolean deleteTempFilesByPrefix(String prefix) {
        return executeWithFailover(new StorageOperation<Boolean>() {
            @Override
            public Boolean execute(StorageService service) {
                return service.deleteTempFilesByPrefix(prefix);
            }
        });
    }

    /**
     * 执行存储操作并支持故障转移
     */
    private <T> T executeWithFailover(StorageOperation<T> operation) {
        StorageService service = selectStorageServiceInstance();

        if (service == null) {
            throw new RuntimeException("没有可用的存储服务");
        }

        try {
            // 执行操作
            T result = operation.execute(service);

            // 标记成功
            if (currentStorageKey != null) {
                ((StorageHealthCheckServiceImpl) healthCheckService).markStorageAsSuccess(currentStorageKey);
            }

            return result;
        } catch (Exception e) {
            log.error("存储操作失败: {}", e.getMessage());

            // 标记失败
            if (currentStorageKey != null) {
                ((StorageHealthCheckServiceImpl) healthCheckService).markStorageAsFailed(
                        currentStorageKey, e.getMessage());
            }

            // 如果是FAILOVER策略，尝试故障转移
            if (currentStrategy == StorageStrategyEnum.FAILOVER) {
                return tryFailover(operation);
            }

            throw new RuntimeException("存储操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 尝试故障转移
     */
    private <T> T tryFailover(StorageOperation<T> operation) {
        log.info("开始故障转移，尝试其他存储节点");

        // 获取所有健康的存储节点
        List<String> healthyKeys = ((StorageHealthCheckServiceImpl) healthCheckService).getHealthyStorageKeys();

        // 尝试每个健康的存储节点
        for (String key : healthyKeys) {
            // 跳过当前失败的节点
            if (key.equals(currentStorageKey)) {
                continue;
            }

            StorageService service = storageServiceMap.get(key);
            if (service == null) {
                continue;
            }

            try {
                log.info("尝试使用存储节点: {}", key);
                T result = operation.execute(service);

                // 成功，切换到新节点
                currentStorageKey = key;
                log.info("故障转移成功，切换到存储节点: {}", key);

                return result;
            } catch (Exception e) {
                log.warn("存储节点操作失败: {}, 错误: {}", key, e.getMessage());
                ((StorageHealthCheckServiceImpl) healthCheckService).markStorageAsFailed(
                        key, e.getMessage());
            }
        }

        // 所有节点都失败
        throw new RuntimeException("所有存储节点均不可用");
    }

    @Override
    public StorageService getStorageService() {
        String key = selectStorageKey();
        return key != null ? storageServiceMap.get(key) : null;
    }

    // ========== StorageManager 接口实现 ==========

    @Override
    public StorageService getStorageService(String key) {
        return storageServiceMap.get(key);
    }

    @Override
    public List<StorageHealthInfo> getStorageHealthList() {
        return healthCheckService.getAllHealthStatus();
    }

    @Override
    public boolean switchStorage(String key) {
        if (key == null || !storageServiceMap.containsKey(key)) {
            log.warn("切换存储节点失败，节点不存在: {}", key);
            return false;
        }

        currentStorageKey = key;
        currentStrategy = StorageStrategyEnum.DEFAULT;
        log.info("手动切换存储节点成功: {}", key);
        return true;
    }

    @Override
    public boolean setStorageStrategy(StorageStrategyEnum strategy) {
        if (strategy == null) {
            log.warn("设置存储策略失败，策略为null");
            return false;
        }

        currentStrategy = strategy;
        log.info("设置存储策略成功: {}", strategy);
        return true;
    }

    @Override
    public StorageStrategyEnum getStorageStrategy() {
        return currentStrategy;
    }

    @Override
    public String getCurrentStorageKey() {
        return currentStorageKey;
    }

    /**
     * 根据当前策略选择存储节点标识
     *
     * @return 存储节点标识
     */
    private String selectStorageKey() {
        return switch (currentStrategy) {
            case FAILOVER -> selectStorageServiceWithFailover();
            case ROUND_ROBIN -> selectStorageServiceByRoundRobin();
            case RANDOM -> selectStorageServiceByRandom();
            default -> selectStorageServiceByDefault();
        };
    }

    // ========== 内部方法 ==========

    /**
     * 根据策略选择存储服务实例
     *
     * @return 存储服务实例
     */
    private StorageService selectStorageServiceInstance() {
        String key = selectStorageKey();
        return key != null ? storageServiceMap.get(key) : null;
    }

    /**
     * DEFAULT策略：使用配置的默认存储节点
     *
     * @return 存储节点标识
     */
    private String selectStorageServiceByDefault() {
        if (currentStorageKey != null) {
            return currentStorageKey;
        }

        // 如果没有当前节点，初始化
        initializeCurrentStorage();
        return currentStorageKey;
    }

    /**
     * FAILOVER策略：选择第一个健康的存储节点
     *
     * @return 存储节点标识
     */
    private String selectStorageServiceWithFailover() {
        List<String> healthyKeys = ((StorageHealthCheckServiceImpl) healthCheckService).getHealthyStorageKeys();

        if (!healthyKeys.isEmpty()) {
            // 返回第一个健康的节点
            String key = healthyKeys.get(0);
            currentStorageKey = key;
            return key;
        }

        // 如果没有健康的节点，使用当前节点（可能不健康）
        return currentStorageKey;
    }

    /**
     * ROUND_ROBIN策略：轮询选择存储节点
     *
     * @return 存储节点标识
     */
    private String selectStorageServiceByRoundRobin() {
        if (!managerProperties.getEnableLoadBalancing()) {
            return selectStorageServiceByDefault();
        }

        List<String> healthyKeys = ((StorageHealthCheckServiceImpl) healthCheckService).getHealthyStorageKeys();

        if (healthyKeys.isEmpty()) {
            return currentStorageKey;
        }

        // 轮询选择
        int index = roundRobinCounter.getAndIncrement() % healthyKeys.size();
        String key = healthyKeys.get(index);
        currentStorageKey = key;
        return key;
    }

    /**
     * RANDOM策略：随机选择存储节点
     *
     * @return 存储节点标识
     */
    private String selectStorageServiceByRandom() {
        if (!managerProperties.getEnableLoadBalancing()) {
            return selectStorageServiceByDefault();
        }

        List<String> healthyKeys = ((StorageHealthCheckServiceImpl) healthCheckService).getHealthyStorageKeys();

        if (healthyKeys.isEmpty()) {
            return currentStorageKey;
        }

        // 随机选择
        int index = random.nextInt(healthyKeys.size());
        String key = healthyKeys.get(index);
        currentStorageKey = key;
        return key;
    }

    /**
     * 存储操作接口
     */
    private interface StorageOperation<T> {
        T execute(StorageService service);
    }
}

