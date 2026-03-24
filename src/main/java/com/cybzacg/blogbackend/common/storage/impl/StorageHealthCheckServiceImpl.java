package com.cybzacg.blogbackend.common.storage.impl;


import com.cybzacg.blogbackend.common.storage.StorageHealthCheckService;
import com.cybzacg.blogbackend.common.storage.StorageHealthInfo;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.StorageManagerProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.storage.StorageHealthStatus;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 存储健康检查服务实现
 */
@Slf4j
public class StorageHealthCheckServiceImpl implements StorageHealthCheckService {
    /**
     * 测试文件内容
     */
    private static final String HEALTH_CHECK_CONTENT = "Storage health check test file";
    private static final String HEALTH_CHECK_FILE_PREFIX = "health-check/";
    private final Map<String, StorageService> storageServiceMap;
    private final StorageProperties storageProperties;
    private final StorageManagerProperties managerProperties;
    private final ScheduledExecutorService scheduler;
    /**
     * 短任务线程池，用于并发执行健康检查
     */
    private final Executor executor;
    /**
     * 健康状态缓存
     */
    private final ConcurrentHashMap<String, StorageHealthInfo> healthStatusMap;
    /**
     * 定时任务句柄
     */
    private ScheduledFuture<?> scheduledTask;

    public StorageHealthCheckServiceImpl(
            Map<String, StorageService> storageServiceMap,
            StorageProperties storageProperties,
            StorageManagerProperties managerProperties,
            ScheduledExecutorService scheduler,
            Executor executor) {
        this.storageServiceMap = storageServiceMap;
        this.storageProperties = storageProperties;
        this.managerProperties = managerProperties;
        this.scheduler = scheduler;
        this.executor = executor;
        this.healthStatusMap = new ConcurrentHashMap<>();

        // 初始化所有存储节点的健康状态
        initializeHealthStatus();

        // 如果启用健康检查，启动时先执行一次健康检查，然后启动定时任务
        if (managerProperties.getEnableHealthCheck()) {
            // 启动时立即执行一次健康检查
            log.info("启动时执行存储节点健康检查");
            checkAllStorages();
            // 启动定时任务
            startScheduledCheck();
        }
    }

    /**
     * 初始化所有存储节点的健康状态
     */
    private void initializeHealthStatus() {
        if (storageProperties.getStorage() != null) {
            for (StorageProperties.Storage storage : storageProperties.getStorage()) {
                String key = storage.getKey();
                StorageType storageType = StorageType.valueOf(storage.getType().toUpperCase());

                StorageHealthInfo healthInfo = StorageHealthInfo.builder()
                        .key(key)
                        .storageType(storageType)
                        .status(StorageHealthStatus.UNKNOWN)
                        .lastCheckTime(LocalDateTime.now())
                        .failureCount(0)
                        .successCount(0)
                        .errorMessage(null)
                        .build();

                healthStatusMap.put(key, healthInfo);
                log.info("初始化存储节点健康状态: {}", key);
            }
        }
    }

    /**
     * 并发探测所有存储节点，并在超时窗口内尽量收集完整结果。
     */
    @Override
    public void checkAllStorages() {
        log.info("开始检查所有存储节点的健康状态");

        // 使用线程池并发执行健康检查
        List<CompletableFuture<Void>> futures = storageServiceMap.keySet().stream()
                .map(key -> CompletableFuture.runAsync(() -> {
                    try {
                        checkStorage(key);
                    } catch (Exception e) {
                        log.error("检查存储节点健康状态失败: {}", key, e);
                    }
                }, executor))
                .collect(Collectors.toList());

        // 等待所有检查完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(managerProperties.getHealthCheckTimeout(), TimeUnit.MILLISECONDS);
            log.info("完成所有存储节点健康检查");
        } catch (TimeoutException e) {
            log.warn("存储节点健康检查超时，部分检查未完成");
        } catch (Exception e) {
            log.error("存储节点健康检查异常", e);
        }
    }

    /**
     * 对指定存储节点执行一次健康检查并刷新其健康状态。
     */
    @Override
    public StorageHealthInfo checkStorage(String key) {
        log.info("开始检查存储节点健康状态: {}", key);

        StorageService storageService = storageServiceMap.get(key);
        if (storageService == null) {
            log.warn("存储节点不存在: {}", key);
            return null;
        }

        boolean success = false;
        String errorMessage = null;

        try {
            // 执行健康检查
            success = performHealthCheck(key, storageService);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("健康检查异常: {}", key, e);
        }

        // 更新健康状态
        StorageHealthInfo healthInfo = updateHealthStatus(key, success, errorMessage);

        log.info("存储节点健康检查完成: {}, 状态: {}", key, healthInfo.getStatus());
        return healthInfo;
    }

    /**
     * 执行单个存储节点的健康检查
     *
     * @param key            存储节点标识
     * @param storageService 存储服务实例
     * @return 是否健康
     */
    private boolean performHealthCheck(String key, StorageService storageService) {
        String testFileName = HEALTH_CHECK_FILE_PREFIX + System.currentTimeMillis() + ".txt";

        try {
            // 上传测试文件
            ByteArrayInputStream inputStream = new ByteArrayInputStream(HEALTH_CHECK_CONTENT.getBytes());
            String url = storageService.upload(inputStream, testFileName, "text/plain");

            // 检查文件是否存在
            boolean exists = storageService.exists(testFileName);

            // 删除测试文件
            storageService.delete(testFileName);

            if (exists && url != null && !url.isEmpty()) {
                log.debug("存储节点健康检查成功: {}", key);
                return true;
            } else {
                log.warn("存储节点健康检查失败: {}, 文件不存在或URL为空", key);
                return false;
            }
        } catch (Exception e) {
            log.error("存储节点健康检查失败: {}", key, e);
            throw e;
        }
    }

    /**
     * 更新存储节点的健康状态
     *
     * @param key          存储节点标识
     * @param success      是否成功
     * @param errorMessage 错误信息
     * @return 更新后的健康信息
     */
    private StorageHealthInfo updateHealthStatus(String key, boolean success, String errorMessage) {
        StorageHealthInfo healthInfo = healthStatusMap.get(key);
        if (healthInfo == null) {
            log.warn("存储节点健康状态不存在: {}", key);
            return null;
        }

        synchronized (healthInfo) {
            healthInfo.setLastCheckTime(LocalDateTime.now());

            if (success) {
                // 检查成功
                healthInfo.setSuccessCount(healthInfo.getSuccessCount() + 1);
                healthInfo.setFailureCount(0);
                healthInfo.setErrorMessage(null);

                // 检查是否达到恢复健康的条件
                if (healthInfo.getStatus() == StorageHealthStatus.UNHEALTHY
                        && healthInfo.getSuccessCount() >= managerProperties.getMinSuccessCount()) {
                    healthInfo.setStatus(StorageHealthStatus.HEALTHY);
                    log.info("存储节点恢复健康: {}", key);
                } else if (healthInfo.getStatus() == StorageHealthStatus.UNKNOWN) {
                    healthInfo.setStatus(StorageHealthStatus.HEALTHY);
                    log.info("存储节点标记为健康: {}", key);
                }
            } else {
                // 检查失败
                healthInfo.setFailureCount(healthInfo.getFailureCount() + 1);
                healthInfo.setSuccessCount(0);
                healthInfo.setErrorMessage(errorMessage);

                // 检查是否达到不健康的条件
                if (healthInfo.getFailureCount() >= managerProperties.getMaxFailureCount()) {
                    healthInfo.setStatus(StorageHealthStatus.UNHEALTHY);
                    log.warn("存储节点标记为不健康: {}, 连续失败次数: {}", key, healthInfo.getFailureCount());
                } else if (healthInfo.getStatus() == StorageHealthStatus.UNKNOWN) {
                    healthInfo.setStatus(StorageHealthStatus.UNHEALTHY);
                    log.warn("存储节点标记为不健康: {}", key);
                }
            }

            return healthInfo;
        }
    }

    /**
     * 启动定时健康检查任务，避免重复启动多个调度实例。
     */
    @Override
    public void startScheduledCheck() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            log.warn("定时健康检查任务已在运行");
            return;
        }

        log.info("启动定时健康检查任务，间隔: {}ms", managerProperties.getHealthCheckInterval());

        scheduledTask = scheduler.scheduleAtFixedRate(
                this::checkAllStorages,
                managerProperties.getHealthCheckInterval(),
                managerProperties.getHealthCheckInterval(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stopScheduledCheck() {
        if (scheduledTask == null || scheduledTask.isCancelled()) {
            log.warn("定时健康检查任务未运行");
            return;
        }

        log.info("停止定时健康检查任务");
        scheduledTask.cancel(true);
        scheduledTask = null;
    }

    @Override
    public List<StorageHealthInfo> getAllHealthStatus() {
        return healthStatusMap.values().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .collect(Collectors.toList());
    }

    @Override
    public StorageHealthInfo getHealthStatus(String key) {
        return healthStatusMap.get(key);
    }

    /**
     * 判断存储节点是否健康
     *
     * @param key 存储节点标识
     * @return 是否健康
     */
    public boolean isStorageHealthy(String key) {
        StorageHealthInfo healthInfo = healthStatusMap.get(key);
        if (healthInfo == null) {
            return false;
        }
        return healthInfo.getStatus() == StorageHealthStatus.HEALTHY;
    }

    /**
     * 获取所有健康的存储节点标识
     *
     * @return 健康的存储节点标识列表
     */
    public List<String> getHealthyStorageKeys() {
        return healthStatusMap.entrySet().stream()
                .filter(entry -> entry.getValue().getStatus() == StorageHealthStatus.HEALTHY)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 标记存储节点为失败（由存储管理器调用）
     *
     * @param key          存储节点标识
     * @param errorMessage 错误信息
     */
    public void markStorageAsFailed(String key, String errorMessage) {
        updateHealthStatus(key, false, errorMessage);
    }

    /**
     * 标记存储节点为成功（由存储管理器调用）
     *
     * @param key 存储节点标识
     */
    public void markStorageAsSuccess(String key) {
        updateHealthStatus(key, true, null);
    }
}

