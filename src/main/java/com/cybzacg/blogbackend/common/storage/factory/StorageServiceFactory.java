package com.cybzacg.blogbackend.common.storage.factory;


import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.storage.StorageType;

/**
 * 存储服务工厂接口。<p>定义创建存储服务实例的统一契约，便于扩展新的存储类型。
 *
 * @author System
 */
public interface StorageServiceFactory {

    /**
     * 创建存储服务实例
     *
     * @param storageConfig        存储配置
     * @param fileUploadProperties 文件上传配置
     * @return StorageService 实例
     */
    StorageService createStorageService(StorageProperties.Storage storageConfig, FileUploadProperties fileUploadProperties);

    /**
     * 获取支持的存储类型
     *
     * @return 存储类型枚举
     */
    StorageType getSupportedStorageType();
}

