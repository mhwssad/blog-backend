package com.cybzacg.blogbackend.common.storage;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import java.io.InputStream;
import java.util.List;
/**
 * 存储服务抽象。
 * 统一封装正式文件、临时文件和分片合并等基础能力，屏蔽底层存储差异。
 */
public interface StorageService {
    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param objectName  存储对象名称（文件路径）
     * @return 文件访问URL
     */
    String upload(InputStream inputStream, String objectName);
    /**
     * 上传文件（带文件类型）
     *
     * @param inputStream 文件输入流
     * @param objectName  存储对象名称（文件路径）
     * @param contentType 文件类型（MIME类型）
     * @return 文件访问URL
     */
    String upload(InputStream inputStream, String objectName, String contentType);
    /**
     * 下载文件
     *
     * @param objectName 存储对象名称（文件路径）
     * @return 文件输入流
     */
    InputStream download(String objectName);
    /**
     * 删除文件
     *
     * @param objectName 存储对象名称（文件路径）
     * @return 是否删除成功
     */
    boolean delete(String objectName);
    /**
     * 批量删除文件
     *
     * @param objectNames 存储对象名称列表
     * @return 删除成功的数量
     */
    int deleteBatch(List<String> objectNames);
    /**
     * 检查文件是否存在
     *
     * @param objectName 存储对象名称（文件路径）
     * @return 是否存在
     */
    boolean exists(String objectName);
    /**
     * 获取文件访问URL
     *
     * @param objectName 存储对象名称（文件路径）
     * @return 文件访问URL
     */
    String getUrl(String objectName);
    /**
     * 获取存储类型
     *
     * @return 存储类型枚举
     */
    StorageType getStorageType();
    /**
     * 上传文件到临时存储空间
     *
     * @param inputStream 文件输入流
     * @param objectName  存储对象名称（文件路径）
     * @return 存储对象名称
     */
    String uploadToTemp(InputStream inputStream, String objectName);
    /**
     * 上传文件到临时存储空间（带文件类型）
     *
     * @param inputStream 文件输入流
     * @param objectName  存储对象名称（文件路径）
     * @param contentType 文件类型（MIME类型）
     * @return 存储对象名称
     */
    String uploadToTemp(InputStream inputStream, String objectName, String contentType);
    /**
     * 合并多个文件（用于分片合并）
     *
     * @param sourceObjectNames 源文件对象名称列表（按顺序）
     * @param targetObjectName  目标文件对象名称
     * @return 是否合并成功
     */
    boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName);
    /**
     * 删除临时文件
     *
     * @param uploadId 上传任务ID
     * @return 是否删除成功
     */
    boolean deleteTempFiles(String uploadId);
    /**
     * 删除临时文件（根据前缀）
     *
     * @param prefix 对象名称前缀
     * @return 是否删除成功
     */
    boolean deleteTempFilesByPrefix(String prefix);
}
