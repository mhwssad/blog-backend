package com.cybzacg.blogbackend.common.storage.factory;



import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.common.storage.impl.LocalStorageServiceImpl;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import org.springframework.stereotype.Component;

/**
 * 本地存储服务工厂。<p>负责创建本地文件系统存储服务实例。
 *
 * @author System
 */
@Component
public class LocalStorageServiceFactory implements StorageServiceFactory {

    @Override
    public StorageService createStorageService(StorageProperties.Storage storageConfig, FileUploadProperties fileUploadProperties) {
        return new LocalStorageServiceImpl(storageConfig, fileUploadProperties);
    }

    @Override
    public StorageType getSupportedStorageType() {
        return StorageType.LOCAL;
    }
}

