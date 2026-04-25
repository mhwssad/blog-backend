package com.cybzacg.blogbackend.common.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;

import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.config.property.StorageProperties;
import com.cybzacg.blogbackend.enums.error.StorageResultCode;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import com.cybzacg.blogbackend.exception.StorageException;
import com.cybzacg.blogbackend.utils.InputStreamUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 阿里云 OSS 对象存储实现。<p>通过 OSS Java SDK 实现文件的上传、下载、删除和分片合并等操作。
 */
@Slf4j
public class OssStorageServiceImpl implements StorageService {
    private final OSS ossClient;
    private final String bucketName;
    private final String baseUrl;
    private final FileUploadProperties fileUploadProperties;

    /**
     * 初始化 OSS 客户端并确保目标桶存在。
     */
    public OssStorageServiceImpl(StorageProperties.Storage storageConfig, FileUploadProperties fileUploadProperties) {
        this.fileUploadProperties = fileUploadProperties;
        this.bucketName = storageConfig.getBucketName();
        this.baseUrl = StringUtils.defaultIfBlank(storageConfig.getBaseUrl(), "");

        // 初始化 OSS 客户端
        if (StringUtils.isNotBlank(storageConfig.getEndpoint())
                && StringUtils.isNotBlank(storageConfig.getAccessKey())
                && StringUtils.isNotBlank(storageConfig.getAccessKeySecret())) {
            this.ossClient = new OSSClientBuilder().build(
                    storageConfig.getEndpoint(),
                    storageConfig.getAccessKey(),
                    storageConfig.getAccessKeySecret()
            );

            // 检查存储桶是否存在，不存在则创建
            if (!ossClient.doesBucketExist(bucketName)) {
                ossClient.createBucket(bucketName);
                log.info("创建 OSS 存储桶成功: {}", bucketName);
            }
        } else {
            throw new StorageException("OSS 配置不完整，endpoint、accessKey、accessKeySecret 不能为空");
        }
    }

    /** 委托给带 contentType 的重载方法，contentType 传 {@code null}。 */
    @Override
    public String upload(InputStream inputStream, String objectName) {
        return upload(inputStream, objectName, null);
    }

    /**
     * 上传对象到 OSS，并按需附带内容类型元数据。
     */
    @Override
    public String upload(InputStream inputStream, String objectName, String contentType) {
        try {
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);

            // 设置内容类型
            if (StringUtils.isNotBlank(contentType)) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(contentType);
                putObjectRequest.setMetadata(metadata);
            }

            // 上传文件
            PutObjectResult result = ossClient.putObject(putObjectRequest);

            log.info("文件上传成功: {}", objectName);
            return getUrl(objectName);
        } catch (Exception e) {
            log.error("文件上传失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.UPLOAD_FAILED);
        }
    }

    /** 下载 OSS 对象并返回其内容流。 */
    @Override
    public InputStream download(String objectName) {
        try {
            OSSObject ossObject = ossClient.getObject(bucketName, objectName);
            log.info("文件下载成功: {}", objectName);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("文件下载失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.GET_FILE_STREAM_FAILED);
        }
    }

    /** 删除单个 OSS 对象。 */
    @Override
    public boolean delete(String objectName) {
        try {
            ossClient.deleteObject(bucketName, objectName);
            log.info("文件删除成功: {}", objectName);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.DELETE_FAILED);
        }
    }

    /**
     * 批量删除 OSS 对象，并返回实际删除数量。
     */
    @Override
    public int deleteBatch(List<String> objectNames) {
        try {
            List<String> keys = new ArrayList<>(objectNames);

            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
            deleteObjectsRequest.setKeys(keys);

            DeleteObjectsResult result = ossClient.deleteObjects(deleteObjectsRequest);
            log.info("批量删除文件成功，删除数量: {}", result.getDeletedObjects().size());
            return result.getDeletedObjects().size();
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            throw new StorageException(StorageResultCode.BATCH_DELETE_FAILED);
        }
    }

    /** 检查 OSS 对象是否存在。 */
    @Override
    public boolean exists(String objectName) {
        try {
            return ossClient.doesObjectExist(bucketName, objectName);
        } catch (Exception e) {
            log.error("检查文件存在性失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.FILE_NOT_EXIST);
        }
    }

    /**
     * 生成 OSS 对象访问地址；未配置固定域名时回退为临时签名 URL。
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
            Date expiration = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("生成文件 URL 失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.GET_DOWNLOAD_URL_FAILED);
        }
    }

    /** 返回 {@link StorageType#OSS}。 */
    @Override
    public StorageType getStorageType() {
        return StorageType.OSS;
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
     * 将多个 OSS 分片顺序写入临时文件，再流式上传目标文件，避免把整份文件加载进内存。
     */
    @Override
    public boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("oss-merge-", ".tmp");
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
     * 按前缀列出并批量删除 OSS 临时对象。
     */
    @Override
    public boolean deleteTempFilesByPrefix(String prefix) {
        try {
            String marker = null;
            int totalCount = 0;

            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix(prefix);
                listObjectsRequest.setMaxKeys(1000);
                listObjectsRequest.setMarker(marker);

                ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
                List<String> keys = new ArrayList<>();
                for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    keys.add(objectSummary.getKey());
                }

                if (!keys.isEmpty()) {
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
                    deleteObjectsRequest.setKeys(keys);
                    DeleteObjectsResult deleteObjectsResult = ossClient.deleteObjects(deleteObjectsRequest);
                    int deletedCount = deleteObjectsResult.getDeletedObjects().size();
                    totalCount += deletedCount;

                    if (deletedCount != keys.size()) {
                        log.warn("删除临时文件存在未完成项，成功删除: {}/{}", deletedCount, keys.size());
                        return false;
                    }
                }

                marker = objectListing.getNextMarker();
                if (!objectListing.isTruncated()) {
                    break;
                }
            } while (true);

            log.info("删除临时文件完成，共 {} 个", totalCount);
            return true;
        } catch (Exception e) {
            log.error("删除临时文件失败: {}", prefix, e);
            throw new StorageException(StorageResultCode.DELETE_FAILED);
        }
    }

    /**
     * 销毁 OSS 客户端
     */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS 客户端已关闭");
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

