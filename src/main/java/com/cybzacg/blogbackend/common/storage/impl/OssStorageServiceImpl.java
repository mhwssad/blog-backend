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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            Date expiration = new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000);
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("生成文件 URL 失败: {}", objectName, e);
            throw new StorageException(StorageResultCode.GET_DOWNLOAD_URL_FAILED);
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.OSS;
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
     * 读取并拼接多个 OSS 分片对象，然后重新上传为目标文件。
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
     * 按前缀列出并批量删除 OSS 临时对象。
     */
    @Override
    public boolean deleteTempFilesByPrefix(String prefix) {
        try {
            // 列出所有匹配前缀的对象
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setPrefix(prefix);
            listObjectsRequest.setMaxKeys(1000);

            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);

            // 删除所有匹配的对象
            if (!objectListing.getObjectSummaries().isEmpty()) {
                List<String> keys = new ArrayList<>();
                for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    keys.add(objectSummary.getKey());
                }

                DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
                deleteObjectsRequest.setKeys(keys);
                ossClient.deleteObjects(deleteObjectsRequest);

                log.info("删除临时文件完成，共 {} 个", keys.size());
            }

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
}

