package com.cybzacg.blogbackend.common.storage.impl;

import com.cybzacg.blogbackend.common.storage.StorageHealthCheckService;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.StorageManagerProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageManagerImplTest {

    @Mock
    private StorageHealthCheckService healthCheckService;

    @Mock
    private StorageService primaryStorageService;

    @Mock
    private StorageService secondaryStorageService;

    private StorageProperties storageProperties;
    private StorageManagerProperties managerProperties;
    private Map<String, StorageService> storageServiceMap;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setStorageType("local");
        storageProperties.setStorage(List.of(buildStorage("primary", "local"), buildStorage("secondary", "local")));

        managerProperties = new StorageManagerProperties();
        managerProperties.setStrategy("FAILOVER");
        managerProperties.setEnableLoadBalancing(true);

        storageServiceMap = new LinkedHashMap<>();
        storageServiceMap.put("primary", primaryStorageService);
        storageServiceMap.put("secondary", secondaryStorageService);
    }

    @Test
    void shouldFailoverToHealthyNodeWhenDownloadFails() throws Exception {
        when(healthCheckService.getHealthyStorageKeys()).thenReturn(List.of("primary", "secondary"));
        when(primaryStorageService.download("article/test.txt"))
                .thenThrow(new RuntimeException("primary unavailable"));
        when(secondaryStorageService.download("article/test.txt"))
                .thenReturn(new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)));

        StorageManagerImpl storageManager = buildStorageManager();

        try (InputStream inputStream = storageManager.download("article/test.txt")) {
            assertEquals("ok", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals("secondary", storageManager.getCurrentStorageKey());
        verify(healthCheckService).markStorageAsFailed(eq("primary"), contains("primary unavailable"));
        verify(healthCheckService).markStorageAsSuccess("secondary");
    }

    @Test
    void shouldNotRetryUploadOnAnotherNodeWithSameInputStream() {
        when(healthCheckService.getHealthyStorageKeys()).thenReturn(List.of("primary", "secondary"));
        when(primaryStorageService.upload(any(InputStream.class), eq("article/test.txt")))
                .thenThrow(new RuntimeException("upload failed"));

        StorageManagerImpl storageManager = buildStorageManager();

        assertThrows(RuntimeException.class,
                () -> storageManager.upload(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)),
                        "article/test.txt"));
        verify(healthCheckService).markStorageAsFailed(eq("primary"), contains("upload failed"));
        verify(secondaryStorageService, never()).upload(any(InputStream.class), eq("article/test.txt"));
        verify(healthCheckService, never()).markStorageAsSuccess("secondary");
    }

    @Test
    void shouldFallbackToDefaultNodeWhenNoHealthyNodeIsReported() {
        when(healthCheckService.getHealthyStorageKeys()).thenReturn(List.of());

        StorageManagerImpl storageManager = buildStorageManager();

        assertSame(primaryStorageService, storageManager.getStorageService());
        assertEquals("primary", storageManager.getCurrentStorageKey());
    }

    private StorageManagerImpl buildStorageManager() {
        return new StorageManagerImpl(storageServiceMap, storageProperties, healthCheckService, managerProperties);
    }

    private StorageProperties.Storage buildStorage(String key, String type) {
        StorageProperties.Storage storage = new StorageProperties.Storage();
        storage.setKey(key);
        storage.setType(type);
        storage.setBucketName("bucket-" + key);
        return storage;
    }
}
