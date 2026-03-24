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
}
