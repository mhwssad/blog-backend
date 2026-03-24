package com.cybzacg.blogbackend.common.storage.factory;


import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.common.storage.impl.OssStorageServiceImpl;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import org.springframework.stereotype.Component;

/**
 * OSS 存储服务工厂
 * 负责创建 OSS 存储服务实例
 *
 * @author System
 */
@Component
public class OssStorageServiceFactory implements StorageServiceFactory {

    @Override
    public StorageService createStorageService(StorageProperties.Storage storageConfig, FileUploadProperties fileUploadProperties) {
        return new OssStorageServiceImpl(storageConfig, fileUploadProperties);
    }

    @Override
    public StorageType getSupportedStorageType() {
        return StorageType.OSS;
    }
}

