package com.cybzacg.blogbackend.module.file;

import com.cybzacg.blogbackend.common.storage.impl.LocalStorageServiceImpl;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.exception.StorageException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalStorageServiceImplTest {
    @Test
    void mergeFilesShouldFailWhenAnyChunkIsMissing() throws Exception {
        Path baseDir = Files.createTempDirectory("file-local-storage-test");

        StorageProperties.Storage storage = new StorageProperties.Storage();
        storage.setType("local");
        storage.setKey("local-test");
        storage.setBucketName(baseDir.toString());

        FileUploadProperties properties = new FileUploadProperties();
        LocalStorageServiceImpl storageService = new LocalStorageServiceImpl(storage, properties);

        storageService.upload(new ByteArrayInputStream("chunk-1".getBytes()), "temp/upload-1/chunk-1.part");
        List<String> chunks = List.of("temp/upload-1/chunk-1.part", "temp/upload-1/chunk-2.part");

        assertThrows(StorageException.class, () -> storageService.mergeFiles(chunks, "avatar/merged.txt"));
        assertFalse(Files.exists(baseDir.resolve("avatar/merged.txt")));
    }
}
