package com.cybzacg.blogbackend.common.storage.impl;


import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.error.StorageResultCode;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import com.cybzacg.blogbackend.exception.StorageException;
import com.cybzacg.blogbackend.utils.FileUtils;
import com.cybzacg.blogbackend.utils.InputStreamUtils;
import com.cybzacg.blogbackend.utils.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * 本地文件系统存储实现。<p>将文件存储在服务器本地磁盘，适用于开发环境或小规模部署场景。
 */
@Slf4j
public class LocalStorageServiceImpl implements StorageService {
    private final String basePath;
    private final String baseUrl;
    private final FileUploadProperties fileUploadProperties;

    /**
     * 根据本地存储配置初始化根目录和访问前缀。
     */
    public LocalStorageServiceImpl(StorageProperties.Storage storageConfig, FileUploadProperties fileUploadProperties) {
        this.basePath = storageConfig.getBucketName();
        this.baseUrl = StringUtils.defaultIfBlank(storageConfig.getBaseUrl(), "");
        this.fileUploadProperties = fileUploadProperties;

        // 初始化存储目录
        FileUtils.createDirectory(basePath);
        log.info("创建本地存储目录: {}", basePath);
    }

    /**
     * 委托给带 contentType 的重载方法，contentType 传 {@code null}。
     */
    @Override
    public String upload(InputStream inputStream, String objectName) {
        return upload(inputStream, objectName, null);
    }

    /**
     * 将输入流写入本地文件系统，并返回对外访问地址。
     */
    @Override
    public String upload(InputStream inputStream, String objectName, String contentType) {
        try {
            // 构建文件路径
            Path filePath = PathUtils.create(basePath, objectName);

            // 确保父目录存在并写入文件
            File targetFile = PathUtils.toFile(filePath);
            FileUtils.createDirectory(targetFile.getParent());
            InputStreamUtils.toFile(inputStream, targetFile);

            log.info("文件上传成功: {}", objectName);
            return getUrl(objectName);
        } catch (Exception e) {
            log.error("文件上传失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.UPLOAD_FAILED);
        } finally {
            InputStreamUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 从本地文件系统读取对象内容，不存在时抛出统一存储异常。
     */
    @Override
    public InputStream download(String objectName) {
        try {
            Path filePath = PathUtils.create(basePath, objectName);
            if (!FileUtils.exists(PathUtils.toFile(filePath))) {
                throw new FileNotFoundException("文件不存在: " + objectName);
            }
            log.info("文件下载成功: {}", objectName);
            return Files.newInputStream(filePath);
        } catch (Exception e) {
            log.error("文件下载失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.GET_FILE_STREAM_FAILED);
        }
    }

    /**
     * 删除单个本地文件，删除失败时统一转换为存储层异常。
     */
    @Override
    public boolean delete(String objectName) {
        try {
            Path filePath = PathUtils.create(basePath, objectName);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("文件删除成功: {}", objectName);
            }
            return deleted;
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.DELETE_FAILED);
        }
    }

    /**
     * 逐个调用 {@link #delete} 完成批量删除，返回成功数量。
     */
    @Override
    public int deleteBatch(List<String> objectNames) {
        int successCount = 0;
        for (String objectName : objectNames) {
            if (delete(objectName)) {
                successCount++;
            }
        }
        log.info("批量删除文件完成，成功删除: {}/{}", successCount, objectNames.size());
        return successCount;
    }

    /**
     * 检查本地文件是否存在，支持 URL 格式和相对路径格式。
     */
    @Override
    public boolean exists(String objectName) {
        try {
            // 提取实际的文件路径
            String actualObjectName = extractObjectName(objectName);
            Path filePath = PathUtils.create(basePath, actualObjectName);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.error("检查文件存在性失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.FILE_NOT_EXIST);
        }
    }

    /**
     * 从 objectName 中提取实际的文件路径
     * 支持 URL 格式（如 http://localhost:8080/temp/test/test-file.zip）和相对路径格式
     *
     * @param objectName 对象名称或 URL
     * @return 实际的文件路径
     */
    private String extractObjectName(String objectName) {
        if (StringUtils.isBlank(objectName)) {
            return objectName;
        }

        // 判断是否为 URL 格式
        if (objectName.startsWith("http://") || objectName.startsWith("https://")) {
            try {
                URI uri = new URI(objectName);
                String path = uri.getPath();

                // 移除路径开头的 /
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                log.debug("从 URL 中提取文件路径: {} -> {}", objectName, path);
                return path;
            } catch (URISyntaxException e) {
                log.warn("URL 解析失败，使用原始字符串: {}", objectName, e);
                return objectName;
            }
        }

        // 如果配置了 baseUrl 且 objectName 包含 baseUrl，则提取路径部分
        if (StringUtils.isNotBlank(baseUrl) && objectName.startsWith(baseUrl)) {
            String path = objectName.substring(baseUrl.length());

            // 移除路径开头的 /
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            log.debug("从完整路径中提取文件路径: {} -> {}", objectName, path);
            return path;
        }

        return objectName;
    }

    /**
     * 根据 baseUrl 或绝对路径生成本地存储对象的访问地址。
     */
    @Override
    public String getUrl(String objectName) {
        // 如果配置了 baseUrl，则使用 baseUrl
        if (StringUtils.isNotBlank(baseUrl)) {
            // 确保 baseUrl 不以 / 结尾，objectName 不以 / 开头
            String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String normalizedObjectName = objectName.startsWith("/") ? objectName.substring(1) : objectName;
            return normalizedBaseUrl + "/" + normalizedObjectName;
        }

        // 否则返回文件绝对路径（仅用于本地开发）
        Path filePath = PathUtils.normalize(PathUtils.create(basePath, objectName));
        return PathUtils.toAbsolutePath(filePath).toString();
    }

    /**
     * 返回 {@link StorageType#LOCAL}。
     */
    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    /**
     * 获取文件大小（字节）
     *
     * @param objectName 存储对象名称
     * @return 文件大小
     */
    public long getFileSize(String objectName) {
        try {
            Path filePath = PathUtils.create(basePath, objectName);
            return FileUtils.getSize(PathUtils.toFile(filePath).getPath());
        } catch (Exception e) {
            log.error("获取文件大小失败: {}", objectName, e);
            return 0;
        }
    }

    /**
     * 复制文件
     *
     * @param sourceObjectName 源文件对象名称
     * @param targetObjectName 目标文件对象名称
     * @return 是否复制成功
     */
    public boolean copyFile(String sourceObjectName, String targetObjectName) {
        try {
            Path sourcePath = PathUtils.create(basePath, sourceObjectName);
            Path targetPath = PathUtils.create(basePath, targetObjectName);

            // 使用FileUtils复制文件
            FileUtils.copyFile(PathUtils.toFile(sourcePath), PathUtils.toFile(targetPath));
            log.info("文件复制成功: {} -> {}", sourceObjectName, targetObjectName);
            return true;
        } catch (Exception e) {
            log.error("文件复制失败: {} -> {}", sourceObjectName, targetObjectName, e);
            return false;
        }
    }

    /**
     * 委托给带 contentType 的重载方法，contentType 传 {@code null}。
     */
    @Override
    public String uploadToTemp(InputStream inputStream, String objectName) {
        return uploadToTemp(inputStream, objectName, null);
    }

    /**
     * 将文件上传到临时目录，路径前缀由配置项 {@code tempDirPrefix} 决定。
     */
    @Override
    public String uploadToTemp(InputStream inputStream, String objectName, String contentType) {
        // 构建临时存储路径：temp/{uploadId}/{objectName}
        String tempObjectName = fileUploadProperties.getTempDirPrefix() + "/" + objectName;
        return upload(inputStream, tempObjectName, contentType);
    }

    /**
     * 按顺序合并本地分片文件，并在成功后清理源分片。
     */
    @Override
    public boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName) {
        Path targetPath = null;
        boolean mergeSuccess = false;
        long totalBytesWritten = 0;
        int processedFiles = 0;

        try {
            targetPath = PathUtils.create(basePath, targetObjectName);

            // 确保目标目录存在
            FileUtils.createDirectory(PathUtils.toFile(targetPath).getParent());

            // 按顺序合并文件
            try (OutputStream outputStream = Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < sourceObjectNames.size(); i++) {
                    String sourceObjectName = sourceObjectNames.get(i);
                    Path sourcePath = PathUtils.create(basePath, sourceObjectName);

                    if (!FileUtils.exists(PathUtils.toFile(sourcePath))) {
                        log.error("分片文件不存在，终止合并: {}", sourceObjectName);
                        throw new StorageException(StorageResultCode.UPLOAD_MERGE_FILES.getCode(), "分片文件不存在: " + sourceObjectName);
                    }

                    try (InputStream inputStream = Files.newInputStream(sourcePath)) {
                        long bytesCopied = InputStreamUtils.copy(inputStream, outputStream);
                        totalBytesWritten += bytesCopied;
                        processedFiles++;
                        log.debug("成功合并分片: {} ({}/{}), 写入字节数: {}", sourceObjectName, i + 1, sourceObjectNames.size(), bytesCopied);
                    } catch (IOException e) {
                        log.error("读取或写入分片文件失败: {}", sourceObjectName, e);
                        throw new StorageException(StorageResultCode.UPLOAD_MERGE_FILES.getCode(), "合并分片文件失败: " + sourceObjectName, e);
                    }
                }

                // 显式刷新输出流，确保所有数据都写入磁盘
                outputStream.flush();
            }

            // 验证合并后的文件大小
            if (processedFiles > 0 && totalBytesWritten == 0) {
                log.error("文件合并异常: 处理了 {} 个分片，但写入的字节数为 0", processedFiles);
                throw new StorageException(StorageResultCode.UPLOAD_MERGE_FILES.getCode(), "文件合并失败：未写入任何数据");
            }

            // 标记合并成功
            mergeSuccess = true;

            // 合并成功后批量删除源文件
            int deletedCount = deleteBatch(sourceObjectNames);

            log.info("文件合并成功: {} -> {}，处理分片数: {}/{}, 写入总字节数: {}, 已删除源文件: {}/{}",
                    sourceObjectNames, targetObjectName, processedFiles, sourceObjectNames.size(),
                    totalBytesWritten, deletedCount, sourceObjectNames.size());
            return true;
        } catch (StorageException e) {
            if (!mergeSuccess && targetPath != null && Files.exists(targetPath)) {
                try {
                    Files.deleteIfExists(targetPath);
                    log.info("已删除部分合并的目标文件: {}", targetObjectName);
                } catch (Exception deleteException) {
                    log.error("删除部分合并的目标文件失败: {}", targetObjectName, deleteException);
                }
            }
            throw e;
        } catch (Exception e) {
            log.error("文件合并失败: {} -> {}", sourceObjectNames, targetObjectName, e);

            // 如果合并过程中出现错误，删除已部分合并的目标文件
            if (!mergeSuccess && targetPath != null && Files.exists(targetPath)) {
                try {
                    Files.deleteIfExists(targetPath);
                    log.info("已删除部分合并的目标文件: {}", targetObjectName);
                } catch (Exception deleteException) {
                    log.error("删除部分合并的目标文件失败: {}", targetObjectName, deleteException);
                }
            }

            throw new StorageException(StorageResultCode.UPLOAD_MERGE_FILES.getCode(), e.getMessage(), e);
        }
    }

    /**
     * 按 uploadId 删除对应的临时文件或目录。
     */
    @Override
    public boolean deleteTempFiles(String uploadId) {
        String prefix = fileUploadProperties.getTempDirPrefix() + "/" + uploadId;
        return deleteTempFilesByPrefix(prefix);
    }

    /**
     * 根据前缀删除临时文件或目录，兼容单文件和目录两种形态。
     */
    @Override
    public boolean deleteTempFilesByPrefix(String prefix) {
        try {
            Path prefixPath = PathUtils.create(basePath, prefix);
            File prefixFile = PathUtils.toFile(prefixPath);

            // 如果是文件，直接删除
            if (FileUtils.isFile(prefixFile.getPath())) {
                FileUtils.deleteFile(prefixFile.getPath());
                log.info("删除临时文件: {}", prefix);
                return true;
            }

            // 如果是目录，递归删除
            if (FileUtils.isDirectory(prefixFile.getPath())) {
                FileUtils.deleteDirectory(prefixFile.getPath());
                log.info("删除临时目录: {}", prefix);
                return true;
            }

            return true;
        } catch (Exception e) {
            log.error("删除临时文件失败: {}", prefix, e);
            throw new StorageException(StorageResultCode.DELETE_FAILED);
        }
    }
}



