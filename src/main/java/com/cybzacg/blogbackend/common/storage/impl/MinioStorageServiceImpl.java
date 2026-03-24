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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

            int successCount = 0;
            for (Result<DeleteError> result : results) {
                try {
                    DeleteError error = result.get();
                    log.error("删除文件失败: {}, 错误: {}", error.objectName(), error.message());
                } catch (Exception e) {
                    log.error("删除文件结果解析失败", e);
                }
                successCount++;
            }

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

    @Override
    public String uploadToTemp(InputStream inputStream, String objectName) {
        return uploadToTemp(inputStream, objectName, null);
    }

    @Override
    public String uploadToTemp(InputStream inputStream, String objectName, String contentType) {
        // 构建临时存储路径：temp/{uploadId}/{objectName}
        String tempObjectName = fileUploadProperties.getTempDirPrefix() + "/" + objectName;
        return upload(inputStream, tempObjectName, contentType);
    }

    /**
     * 读取并拼接多个 MinIO 分片对象，然后重新上传为目标文件。
     */
    @Override
    public boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName) {
        try {
            // 合并文件：读取所有分片并合并
            StringBuilder mergedContent = new StringBuilder();

            for (String sourceObjectName : sourceObjectNames) {
                try (InputStream inputStream = download(sourceObjectName)) {
                    byte[] bytes = InputStreamUtils.toByteArray(inputStream);
                    mergedContent.append(new String(bytes, StandardCharsets.ISO_8859_1));
                }
            }

            // 上传合并后的文件
            byte[] mergedBytes = mergedContent.toString().getBytes(StandardCharsets.ISO_8859_1);
            try (ByteArrayInputStream byteArrayInputStream = InputStreamUtils.fromByteArray(mergedBytes)) {
                upload(byteArrayInputStream, targetObjectName);
            }

            log.info("文件合并成功: {} -> {}", sourceObjectNames, targetObjectName);
            return true;
        } catch (Exception e) {
            log.error("文件合并失败: {} -> {}", sourceObjectNames, targetObjectName, e);
            throw new StorageException(StorageResultCode.UPLOAD_MERGE_FILES);
        }
    }

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
            // 列出所有匹配前缀的对象
            List<DeleteObject> deleteObjects = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                deleteObjects.add(new DeleteObject(item.objectName()));
            }

            // 批量删除
            if (!deleteObjects.isEmpty()) {
                minioClient.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(bucketName)
                                .objects(deleteObjects)
                                .build()
                );
                log.info("删除临时文件完成，共 {} 个", deleteObjects.size());
            }

            return true;
        } catch (Exception e) {
            log.error("删除临时文件失败: {}", prefix, e);
            throw new StorageException(StorageResultCode.DELETE_FAILED);
        }
    }
}

