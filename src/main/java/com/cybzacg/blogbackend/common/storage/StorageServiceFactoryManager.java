package com.cybzacg.blogbackend.common.storage;
import com.cybzacg.blogbackend.common.storage.factory.StorageServiceFactory;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 存储服务工厂管理器。
 * 负责注册不同存储类型对应的工厂，并在配置装配阶段创建具体存储实例。
 */
@Slf4j
@Component
public class StorageServiceFactoryManager {
    private final List<StorageServiceFactory> factories;
    private final Map<StorageType, StorageServiceFactory> factoryMap = new ConcurrentHashMap<>();
    public StorageServiceFactoryManager(List<StorageServiceFactory> factories) {
        this.factories = factories;
    }
    /**
     * 初始化工厂映射
     * 在 Bean 初始化后执行，将所有工厂按存储类型注册到 Map 中
     */
    @PostConstruct
    public void init() {
        for (StorageServiceFactory factory : factories) {
            StorageType storageType = factory.getSupportedStorageType();
            factoryMap.put(storageType, factory);
            log.info("注册存储服务工厂: {}", storageType.getValue());
        }
        log.info("存储服务工厂管理器初始化完成，共注册 {} 个工厂", factoryMap.size());
    }
    /**
     * 根据存储类型创建存储服务实例
     *
     * @param storageType          存储类型枚举
     * @param storageConfig        存储配置
     * @param fileUploadProperties 文件上传配置
     * @return StorageService 实例
     * @throws IllegalArgumentException 当存储类型不支持时抛出
     */
    public StorageService createStorageService(StorageType storageType,
                                               StorageProperties.Storage storageConfig,
                                               FileUploadProperties fileUploadProperties) {
        StorageServiceFactory factory = factoryMap.get(storageType);
        if (factory == null) {
            log.error("不支持的存储类型: {}", storageType.getValue());
            throw new IllegalArgumentException("不支持的存储类型: " + storageType.getValue());
        }
        log.info("使用工厂创建存储服务: {}", storageType.getValue());
        return factory.createStorageService(storageConfig, fileUploadProperties);
    }
    /**
     * 根据存储类型代码创建存储服务实例
     *
     * @param storageTypeCode      存储类型代码（如 oss、cos、local、minio）
     * @param storageConfig        存储配置
     * @param fileUploadProperties 文件上传配置
     * @return StorageService 实例
     * @throws IllegalArgumentException 当存储类型不支持时抛出
     */
    public StorageService createStorageService(String storageTypeCode,
                                               StorageProperties.Storage storageConfig,
                                               FileUploadProperties fileUploadProperties) {
        StorageType storageType = StorageType.fromCode(storageTypeCode);
        return createStorageService(storageType, storageConfig, fileUploadProperties);
    }
    /**
     * 获取所有已注册的存储类型
     *
     * @return 存储类型与工厂的映射
     */
    public Map<StorageType, StorageServiceFactory> getRegisteredFactories() {
        return new HashMap<>(factoryMap);
    }
    /**
     * 检查是否支持指定的存储类型
     *
     * @param storageType 存储类型枚举
     * @return 是否支持
     */
    public boolean isSupported(StorageType storageType) {
        return factoryMap.containsKey(storageType);
    }
    /**
     * 检查是否支持指定的存储类型
     *
     * @param storageTypeCode 存储类型代码
     * @return 是否支持
     */
    public boolean isSupported(String storageTypeCode) {
        try {
            StorageType storageType = StorageType.fromCode(storageTypeCode);
            return factoryMap.containsKey(storageType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
