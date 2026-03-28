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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    private final List<String> storageKeys;

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
     * 当前使用的存储节点标识
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
        this.storageKeys = buildStorageKeys();
        this.currentStorageKey = resolveDefaultStorageKey();

        log.info("存储管理器初始化完成，策略: {}, 当前存储节点: {}",
                currentStrategy, currentStorageKey);
    }

    @Override
    public String upload(InputStream inputStream, String objectName) {
        return executeOnSelectedStorage(service -> service.upload(inputStream, objectName), false);
    }

    @Override
    public String upload(InputStream inputStream, String objectName, String contentType) {
        return executeOnSelectedStorage(service -> service.upload(inputStream, objectName, contentType), false);
    }

    @Override
    public InputStream download(String objectName) {
        return executeOnSelectedStorage(service -> service.download(objectName), true);
    }

    @Override
    public boolean delete(String objectName) {
        return executeOnSelectedStorage(service -> service.delete(objectName), true);
    }

    @Override
    public int deleteBatch(List<String> objectNames) {
        return executeOnSelectedStorage(service -> service.deleteBatch(objectNames), true);
    }

    @Override
    public boolean exists(String objectName) {
        return executeOnSelectedStorage(service -> service.exists(objectName), true);
    }

    @Override
    public String getUrl(String objectName) {
        return executeOnSelectedStorage(service -> service.getUrl(objectName), true);
    }

    @Override
    public StorageType getStorageType() {
        SelectedStorage selectedStorage = selectStorageService();
        return selectedStorage != null ? selectedStorage.service().getStorageType() : null;
    }

    @Override
    public String uploadToTemp(InputStream inputStream, String objectName) {
        return executeOnSelectedStorage(service -> service.uploadToTemp(inputStream, objectName), false);
    }

    @Override
    public String uploadToTemp(InputStream inputStream, String objectName, String contentType) {
        return executeOnSelectedStorage(service -> service.uploadToTemp(inputStream, objectName, contentType), false);
    }

    @Override
    public boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName) {
        return executeOnSelectedStorage(service -> service.mergeFiles(sourceObjectNames, targetObjectName), true);
    }

    @Override
    public boolean deleteTempFiles(String uploadId) {
        return executeOnSelectedStorage(service -> service.deleteTempFiles(uploadId), true);
    }

    @Override
    public boolean deleteTempFilesByPrefix(String prefix) {
        return executeOnSelectedStorage(service -> service.deleteTempFilesByPrefix(prefix), true);
    }

    /**
     * 在选中的节点上执行一次存储操作，并按策略决定是否允许切换到其他节点重试。
     * 上传类操作依赖单次消费输入流，因此只标记失败，不在管理器层复用同一个流做自动重试。
     *
     * @param operation      存储操作
     * @param allowFailover  是否允许切换到其他节点重试
     * @param <T>            返回值类型
     * @return 操作结果
     */
    private <T> T executeOnSelectedStorage(StorageOperation<T> operation, boolean allowFailover) {
        SelectedStorage selectedStorage = selectStorageService();
        if (selectedStorage == null) {
            throw new RuntimeException("没有可用的存储服务");
        }

        try {
            T result = operation.execute(selectedStorage.service());
            healthCheckService.markStorageAsSuccess(selectedStorage.key());
            return result;
        } catch (Exception e) {
            log.error("存储操作失败，节点: {}, 错误: {}", selectedStorage.key(), e.getMessage(), e);
            healthCheckService.markStorageAsFailed(selectedStorage.key(), e.getMessage());

            if (allowFailover && currentStrategy == StorageStrategyEnum.FAILOVER) {
                return tryFailover(operation, selectedStorage.key());
            }
            throw wrapStorageException(e);
        }
    }

    /**
     * 在故障转移策略下尝试使用其他节点继续执行本次操作。
     *
     * @param operation  存储操作
     * @param failedKey  已失败的节点标识
     * @param <T>        返回值类型
     * @return 操作结果
     */
    private <T> T tryFailover(StorageOperation<T> operation, String failedKey) {
        log.info("开始故障转移，失败节点: {}", failedKey);

        List<String> candidateKeys = buildFailoverCandidateKeys(failedKey);
        for (String key : candidateKeys) {
            StorageService storageService = storageServiceMap.get(key);
            if (storageService == null) {
                continue;
            }

            try {
                log.info("尝试使用故障转移节点: {}", key);
                T result = operation.execute(storageService);
                healthCheckService.markStorageAsSuccess(key);
                currentStorageKey = key;
                log.info("故障转移成功，切换到存储节点: {}", key);
                return result;
            } catch (Exception e) {
                log.warn("故障转移节点执行失败: {}, 错误: {}", key, e.getMessage(), e);
                healthCheckService.markStorageAsFailed(key, e.getMessage());
            }
        }

        throw new RuntimeException("所有存储节点均不可用");
    }

    /**
     * 组装故障转移候选节点列表。
     * 优先尝试健康节点，随后再按配置顺序兜底其他节点，以避免健康检查关闭时直接无节点可用。
     *
     * @param failedKey 已失败节点
     * @return 候选节点列表
     */
    private List<String> buildFailoverCandidateKeys(String failedKey) {
        Set<String> candidateKeys = new LinkedHashSet<>();
        for (String key : getHealthyStorageKeys()) {
            if (!key.equals(failedKey)) {
                candidateKeys.add(key);
            }
        }
        for (String key : storageKeys) {
            if (!key.equals(failedKey) && storageServiceMap.containsKey(key)) {
                candidateKeys.add(key);
            }
        }
        return new ArrayList<>(candidateKeys);
    }

    /**
     * 将任意异常统一转换为运行时异常，同时保留已有业务异常类型。
     *
     * @param exception 原始异常
     * @return 运行时异常
     */
    private RuntimeException wrapStorageException(Exception exception) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException("存储操作失败: " + exception.getMessage(), exception);
    }

    @Override
    public StorageService getStorageService() {
        SelectedStorage selectedStorage = selectStorageService();
        return selectedStorage != null ? selectedStorage.service() : null;
    }

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
     * 根据当前策略选择节点。
     *
     * @return 选中的节点标识
     */
    private String selectStorageKey() {
        return switch (currentStrategy) {
            case FAILOVER -> selectStorageServiceWithFailover();
            case ROUND_ROBIN -> selectStorageServiceByRoundRobin();
            case RANDOM -> selectStorageServiceByRandom();
            default -> selectStorageServiceByDefault();
        };
    }

    /**
     * 选择本次调用实际使用的存储节点和服务实例。
     *
     * @return 节点与服务的组合，不存在返回 null
     */
    private SelectedStorage selectStorageService() {
        String key = selectStorageKey();
        if (key != null && storageServiceMap.containsKey(key)) {
            currentStorageKey = key;
            return new SelectedStorage(key, storageServiceMap.get(key));
        }

        for (String fallbackKey : storageKeys) {
            StorageService storageService = storageServiceMap.get(fallbackKey);
            if (storageService != null) {
                currentStorageKey = fallbackKey;
                return new SelectedStorage(fallbackKey, storageService);
            }
        }
        return null;
    }

    /**
     * DEFAULT 策略优先使用当前节点，否则回退到配置中的默认节点。
     *
     * @return 默认节点标识
     */
    private String selectStorageServiceByDefault() {
        if (currentStorageKey != null && storageServiceMap.containsKey(currentStorageKey)) {
            return currentStorageKey;
        }

        String defaultKey = resolveDefaultStorageKey();
        if (defaultKey != null) {
            currentStorageKey = defaultKey;
        }
        return defaultKey;
    }

    /**
     * FAILOVER 策略优先使用健康的当前节点，否则切换到首个健康节点，最后兜底默认节点。
     *
     * @return 故障转移策略下的节点标识
     */
    private String selectStorageServiceWithFailover() {
        List<String> healthyKeys = getHealthyStorageKeys();
        if (!healthyKeys.isEmpty()) {
            if (currentStorageKey != null && healthyKeys.contains(currentStorageKey)) {
                return currentStorageKey;
            }
            return healthyKeys.get(0);
        }
        return selectStorageServiceByDefault();
    }

    /**
     * ROUND_ROBIN 策略在健康节点列表中轮询。
     *
     * @return 轮询选中的节点标识
     */
    private String selectStorageServiceByRoundRobin() {
        if (!Boolean.TRUE.equals(managerProperties.getEnableLoadBalancing())) {
            return selectStorageServiceByDefault();
        }

        List<String> healthyKeys = getHealthyStorageKeys();
        if (healthyKeys.isEmpty()) {
            return selectStorageServiceByDefault();
        }

        int index = Math.floorMod(roundRobinCounter.getAndIncrement(), healthyKeys.size());
        return healthyKeys.get(index);
    }

    /**
     * RANDOM 策略在健康节点列表中随机选择。
     *
     * @return 随机选中的节点标识
     */
    private String selectStorageServiceByRandom() {
        if (!Boolean.TRUE.equals(managerProperties.getEnableLoadBalancing())) {
            return selectStorageServiceByDefault();
        }

        List<String> healthyKeys = getHealthyStorageKeys();
        if (healthyKeys.isEmpty()) {
            return selectStorageServiceByDefault();
        }

        return healthyKeys.get(random.nextInt(healthyKeys.size()));
    }

    /**
     * 构建稳定的节点顺序，优先沿用配置顺序，避免直接依赖 HashMap 的遍历结果。
     *
     * @return 节点顺序列表
     */
    private List<String> buildStorageKeys() {
        Set<String> orderedKeys = new LinkedHashSet<>();
        if (storageProperties.getStorage() != null) {
            for (StorageProperties.Storage storage : storageProperties.getStorage()) {
                if (storageServiceMap.containsKey(storage.getKey())) {
                    orderedKeys.add(storage.getKey());
                }
            }
        }
        orderedKeys.addAll(storageServiceMap.keySet());
        return new ArrayList<>(orderedKeys);
    }

    /**
     * 解析配置中的默认节点，用于健康节点不可用时兜底。
     *
     * @return 默认节点标识
     */
    private String resolveDefaultStorageKey() {
        String defaultStorageType = storageProperties.getStorageType();
        if (storageProperties.getStorage() != null) {
            for (StorageProperties.Storage storage : storageProperties.getStorage()) {
                if (storageServiceMap.containsKey(storage.getKey())
                        && storage.getType().equalsIgnoreCase(defaultStorageType)) {
                    return storage.getKey();
                }
            }
        }
        return storageKeys.isEmpty() ? null : storageKeys.get(0);
    }

    /**
     * 读取当前健康节点列表，并过滤掉未注册的节点标识。
     *
     * @return 健康节点列表
     */
    private List<String> getHealthyStorageKeys() {
        return healthCheckService.getHealthyStorageKeys().stream()
                .filter(storageServiceMap::containsKey)
                .toList();
    }

    /**
     * 存储操作接口
     */
    private interface StorageOperation<T> {
        T execute(StorageService service);
    }

    /**
     * 单次调用选择出的节点与服务实例。
     *
     * @param key     节点标识
     * @param service 存储服务
     */
    private record SelectedStorage(String key, StorageService service) {
    }
}
