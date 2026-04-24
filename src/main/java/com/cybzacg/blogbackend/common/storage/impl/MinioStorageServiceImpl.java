package com.cybzacg.blogbackend.common.storage.impl;


import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.error.StorageResultCode;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import com.cybzacg.blogbackend.exception.StorageException;
import com.cybzacg.blogbackend.utils.InputStreamUtils;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储实现。<p>通过 MinIO Java SDK 实现文件的上传、下载、删除和分片合并等操作。
 */
@Slf4j
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final String bucketName;
    private final String baseUrl;
    private final FileUploadProperties fileUploadProperties;

    /**
     * 初始化 MinIO 客户端并确保目标桶存在。
     */
    public MinioStorageServiceImpl(StorageProperties.Storage storageConfig, FileUploadProperties fileUploadProperties) {
        this.fileUploadProperties = fileUploadProperties;
        this.bucketName = storageConfig.getBucketName();
        this.baseUrl = StringUtils.defaultIfBlank(storageConfig.getBaseUrl(), "");

        // 初始化 MinIO 客户端
        if (StringUtils.isNotBlank(storageConfig.getEndpoint())
                && StringUtils.isNotBlank(storageConfig.getAccessKey())
                && StringUtils.isNotBlank(storageConfig.getAccessKeySecret())) {

            // 构建MinIO客户端，支持自定义endpoint
            String endpoint = storageConfig.getEndpoint();

            this.minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(storageConfig.getAccessKey(), storageConfig.getAccessKeySecret())
                    .build();

            // 检查存储桶是否存在，不存在则创建
            try {
                if (!minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build())) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
                    log.info("创建 MinIO 存储桶成功: {}", bucketName);

                    // 设置存储桶为公开读取（可选）
                    // minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    //         .bucket(bucketName)
                    //         .config(getPublicReadPolicy(bucketName))
                    //         .build());
                }
            } catch (Exception e) {
                throw new StorageException("初始化 MinIO 存储服务失败: " + e.getMessage(), e);
            }
        } else {
            throw new StorageException("MinIO 配置不完整，endpoint、accessKey、accessKeySecret 不能为空");
        }
    }

    /** 委托给带 contentType 的重载方法，contentType 传 {@code null}。 */
    @Override
    public String upload(InputStream inputStream, String objectName) {
        return upload(inputStream, objectName, null);
    }

    /**
     * 上传对象到 MinIO，并按需附带内容类型元数据。
     */
    @Override
    public String upload(InputStream inputStream, String objectName, String contentType) {
        try {
            // 创建上传对象
            PutObjectArgs.Builder putObjectArgsBuilder = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, -1, 10485760); // 10MB part size

            // 设置内容类型
            if (StringUtils.isNotBlank(contentType)) {
                putObjectArgsBuilder.contentType(contentType);
            }

            // 上传文件
            ObjectWriteResponse response = minioClient.putObject(putObjectArgsBuilder.build());

            log.info("文件上传成功: {}, ETag: {}", objectName, response.etag());
            return getUrl(objectName);
        } catch (Exception e) {
            log.error("文件上传失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.UPLOAD_FAILED);
        } finally {
            // 使用InputStreamUtils关闭输入流
            InputStreamUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 读取 MinIO 对象流，供下载或合并流程继续处理。
     */
    @Override
    public InputStream download(String objectName) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("文件下载成功: {}", objectName);
            return response;
        } catch (Exception e) {
            log.error("文件下载失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.GET_FILE_STREAM_FAILED);
        }
    }

    /**
     * 删除单个 MinIO 对象。
     */
    @Override
    public boolean delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("文件删除成功: {}", objectName);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.DELETE_FAILED);
        }
    }

    /**
     * 批量删除 MinIO 对象，并统计本次处理的对象数量。
     */
    @Override
    public int deleteBatch(List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return 0;
        }

        try {
            List<DeleteObject> deleteObjects = new ArrayList<>();
            for (String objectName : objectNames) {
                deleteObjects.add(new DeleteObject(objectName));
            }

            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(deleteObjects)
                            .build()
            );

            int errorCount = 0;
            for (Result<DeleteError> result : results) {
                try {
                    DeleteError error = result.get();
                    log.error("删除文件失败: {}, 错误: {}", error.objectName(), error.message());
                    errorCount++;
                } catch (Exception e) {
                    log.error("删除文件结果解析失败", e);
                    errorCount++;
                }
            }

            int successCount = Math.max(0, objectNames.size() - errorCount);
            log.info("批量删除文件完成，成功删除: {}/{}", successCount, objectNames.size());
            return successCount;
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            throw new StorageException(StorageResultCode.BATCH_DELETE_FAILED);
        }
    }

    /**
     * 通过对象元信息探测 MinIO 中文件是否存在。
     */
    @Override
    public boolean exists(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return stat != null;
        } catch (Exception e) {
            log.error("检查文件存在性失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.FILE_NOT_EXIST);
        }
    }

    /**
     * 生成 MinIO 对象访问地址；未配置直连地址时回退为临时签名 URL。
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

        // 否则使用临时签名 URL（7天有效期）
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成文件 URL 失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.GET_DOWNLOAD_URL_FAILED);
        }
    }

    /** 返回 {@link StorageType#MINIO}。 */
    @Override
    public StorageType getStorageType() {
        return StorageType.MINIO;
    }

    /**
     * 销毁 MinIO 客户端
     */
    @PreDestroy
    public void destroy() {
        // MinIO客户端无需显式关闭，但在某些场景下可能需要清理资源
        log.info("MinIO 客户端已清理");
    }

    /**
     * 获取公开读取策略配置（可选）
     *
     * @param bucketName 存储桶名称
     * @return 策略配置JSON
     */
    private String getPublicReadPolicy(String bucketName) {
        return String.format(
                "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": [\"s3:GetObject\"],\n" +
                        "      \"Resource\": [\"arn:aws:s3:::%s/*\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                bucketName
        );
    }

    /** 委托给带 contentType 的重载方法，contentType 传 {@code null}。 */
    @Override
    public String uploadToTemp(InputStream inputStream, String objectName) {
        return uploadToTemp(inputStream, objectName, null);
    }

    /** 将文件上传到临时目录，路径前缀由配置项 {@code tempDirPrefix} 决定。 */
    @Override
    public String uploadToTemp(InputStream inputStream, String objectName, String contentType) {
        // 构建临时存储路径：temp/{uploadId}/{objectName}
        String tempObjectName = fileUploadProperties.getTempDirPrefix() + "/" + objectName;
        return upload(inputStream, tempObjectName, contentType);
    }

    /**
     * 将多个 MinIO 分片顺序写入临时文件，再流式上传目标文件，避免把整份文件加载进内存。
     */
    @Override
    public boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("minio-merge-", ".tmp");
            mergeSourceFilesToTempFile(sourceObjectNames, tempFile);
            try (InputStream mergedInputStream = Files.newInputStream(tempFile)) {
                upload(mergedInputStream, targetObjectName);
            }

            log.info("文件合并成功: {} -> {}", sourceObjectNames, targetObjectName);
            return true;
        } catch (Exception e) {
            log.error("文件合并失败: {} -> {}", sourceObjectNames, targetObjectName, e);
            throw new StorageException(StorageResultCode.UPLOAD_MERGE_FILES);
        } finally {
            deleteTempMergeFile(tempFile);
        }
    }

    /** 按 uploadId 删除对应的临时分片对象。 */
    @Override
    public boolean deleteTempFiles(String uploadId) {
        String prefix = fileUploadProperties.getTempDirPrefix() + "/" + uploadId + "/";
        return deleteTempFilesByPrefix(prefix);
    }

    /**
     * 按前缀枚举并删除临时分片对象。
     */
    @Override
    public boolean deleteTempFilesByPrefix(String prefix) {
        try {
            List<String> objectNames = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                objectNames.add(item.objectName());
            }

            if (objectNames.isEmpty()) {
                return true;
            }

            int successCount = deleteBatch(objectNames);
            if (successCount != objectNames.size()) {
                log.warn("删除临时文件存在未完成项，成功删除: {}/{}", successCount, objectNames.size());
                return false;
            }

            log.info("删除临时文件完成，共 {} 个", objectNames.size());
            return true;
        } catch (Exception e) {
            log.error("删除临时文件失败: {}", prefix, e);
            throw new StorageException(StorageResultCode.DELETE_FAILED);
        }
    }

    /**
     * 将远端分片顺序写入本地临时文件，避免在 JVM 内存中拼接整个文件内容。
     *
     * @param sourceObjectNames 分片对象列表
     * @param tempFile          临时合并文件
     * @throws Exception 写入失败时抛出
     */
    private void mergeSourceFilesToTempFile(List<String> sourceObjectNames, Path tempFile) throws Exception {
        try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
            for (String sourceObjectName : sourceObjectNames) {
                try (InputStream inputStream = download(sourceObjectName)) {
                    InputStreamUtils.copy(inputStream, outputStream);
                }
            }
        }
    }

    /**
     * 删除本地临时合并文件。
     *
     * @param tempFile 临时文件路径
     */
    private void deleteTempMergeFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (Exception e) {
            log.warn("删除临时合并文件失败: {}", tempFile, e);
        }
    }
}

