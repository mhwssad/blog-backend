package com.cybzacg.blogbackend.common.storage;
import java.util.List;
/**
 * 存储健康检查服务。
 * 负责维护各存储节点的探活结果，并为故障切换提供状态依据。
 */
public interface StorageHealthCheckService {
    /**
     * 检查所有存储节点的健康状态
     */
    void checkAllStorages();
    /**
     * 检查指定存储节点的健康状态
     *
     * @param key 存储节点标识
     * @return 健康信息
     */
    StorageHealthInfo checkStorage(String key);
    /**
     * 启动定时健康检查
     */
    void startScheduledCheck();
    /**
     * 停止定时健康检查
     */
    void stopScheduledCheck();
    /**
     * 获取所有存储节点的健康状态
     *
     * @return 健康信息列表
     */
    List<StorageHealthInfo> getAllHealthStatus();
    /**
     * 获取指定存储节点的健康状态
     *
     * @param key 存储节点标识
     * @return 健康信息，不存在返回null
     */
    StorageHealthInfo getHealthStatus(String key);

    /**
     * 判断指定存储节点当前是否处于健康状态。
     *
     * @param key 存储节点标识
     * @return true 表示健康
     */
    boolean isStorageHealthy(String key);

    /**
     * 获取当前健康的存储节点标识列表。
     *
     * @return 健康节点标识列表
     */
    List<String> getHealthyStorageKeys();

    /**
     * 标记指定节点本次访问失败。
     *
     * @param key 存储节点标识
     * @param errorMessage 错误信息
     */
    void markStorageAsFailed(String key, String errorMessage);

    /**
     * 标记指定节点本次访问成功。
     *
     * @param key 存储节点标识
     */
    void markStorageAsSuccess(String key);
}
