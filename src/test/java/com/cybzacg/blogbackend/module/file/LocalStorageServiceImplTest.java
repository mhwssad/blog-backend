package com.cybzacg.blogbackend.module.file;

import com.cybzacg.blogbackend.common.storage.impl.LocalStorageServiceImpl;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.exception.StorageException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceImplTest {
    @Test
    void uploadShouldPersistFileAndReturnUrl() throws Exception {
        Path baseDir = Files.createTempDirectory("file-local-storage-test");
        LocalStorageServiceImpl storageService = buildStorageService(baseDir, "http://localhost/files");

        String url = storageService.upload(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), "avatar/demo.txt");

        assertEquals("http://localhost/files/avatar/demo.txt", url);
        assertTrue(Files.exists(baseDir.resolve("avatar/demo.txt")));
        assertEquals("hello", Files.readString(baseDir.resolve("avatar/demo.txt")));
    }

    @Test
    void mergeFilesShouldCombineChunksAndDeleteSources() throws Exception {
        Path baseDir = Files.createTempDirectory("file-local-storage-test");
        LocalStorageServiceImpl storageService = buildStorageService(baseDir, "http://localhost/files");

        storageService.upload(new ByteArrayInputStream("chunk-1".getBytes(StandardCharsets.UTF_8)), "temp/upload-1/chunk-1.part");
        storageService.upload(new ByteArrayInputStream("chunk-2".getBytes(StandardCharsets.UTF_8)), "temp/upload-1/chunk-2.part");

        boolean merged = storageService.mergeFiles(
                List.of("temp/upload-1/chunk-1.part", "temp/upload-1/chunk-2.part"),
                "avatar/merged.txt"
        );

        assertTrue(merged);
        assertEquals("chunk-1chunk-2", Files.readString(baseDir.resolve("avatar/merged.txt")));
        assertFalse(Files.exists(baseDir.resolve("temp/upload-1/chunk-1.part")));
        assertFalse(Files.exists(baseDir.resolve("temp/upload-1/chunk-2.part")));
    }

    @Test
    void mergeFilesShouldFailWhenAnyChunkIsMissing() throws Exception {
        Path baseDir = Files.createTempDirectory("file-local-storage-test");
        LocalStorageServiceImpl storageService = buildStorageService(baseDir, "http://localhost/files");

        storageService.upload(new ByteArrayInputStream("chunk-1".getBytes(StandardCharsets.UTF_8)), "temp/upload-1/chunk-1.part");
        List<String> chunks = List.of("temp/upload-1/chunk-1.part", "temp/upload-1/chunk-2.part");

        assertThrows(StorageException.class, () -> storageService.mergeFiles(chunks, "avatar/merged.txt"));
        assertFalse(Files.exists(baseDir.resolve("avatar/merged.txt")));
    }

    @Test
    void deleteShouldRemoveUploadedFile() throws Exception {
        Path baseDir = Files.createTempDirectory("file-local-storage-test");
        LocalStorageServiceImpl storageService = buildStorageService(baseDir, "http://localhost/files");

        storageService.upload(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), "avatar/delete-me.txt");

        assertTrue(storageService.delete("avatar/delete-me.txt"));
        assertFalse(Files.exists(baseDir.resolve("avatar/delete-me.txt")));
    }

    @Test
    void getUrlShouldFallbackToAbsolutePathWhenBaseUrlMissing() throws Exception {
        Path baseDir = Files.createTempDirectory("file-local-storage-test");
        LocalStorageServiceImpl storageService = buildStorageService(baseDir, null);

        String url = storageService.getUrl("avatar/demo.txt");

        assertEquals(baseDir.resolve("avatar/demo.txt").toAbsolutePath().toString(), url);
    }

    @Test
    void deleteTempFilesShouldRemoveUploadTempDirectory() throws Exception {
        Path baseDir = Files.createTempDirectory("file-local-storage-test");
        LocalStorageServiceImpl storageService = buildStorageService(baseDir, "http://localhost/files");

        storageService.uploadToTemp(new ByteArrayInputStream("chunk".getBytes(StandardCharsets.UTF_8)), "upload-2/chunk-1.part");

        assertTrue(storageService.deleteTempFiles("upload-2"));
        assertFalse(Files.exists(baseDir.resolve("temp/upload-2")));
    }

    private LocalStorageServiceImpl buildStorageService(Path baseDir, String baseUrl) {
        StorageProperties.Storage storage = new StorageProperties.Storage();
        storage.setType("local");
        storage.setKey("local-test");
        storage.setBucketName(baseDir.toString());
        storage.setBaseUrl(baseUrl);

        FileUploadProperties properties = new FileUploadProperties();
        properties.setTempDirPrefix("temp");
        return new LocalStorageServiceImpl(storage, properties);
    }
}
