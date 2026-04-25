package com.cybzacg.blogbackend.common.storage;

import com.cybzacg.blogbackend.enums.storage.StorageStrategyEnum;

import java.util.List;

/**
 * 存储管理器。<p>在具体存储实现之上补充路由、策略切换和健康状态查询能力。
 */
public interface StorageManager extends StorageService {
    /**
     * 获取当前活跃的存储服务
     *
     * @return 存储服务实例
     */
    StorageService getStorageService();

    /**
     * 根据节点标识获取指定的存储服务
     *
     * @param key 存储节点标识
     * @return 存储服务实例，不存在返回null
     */
    StorageService getStorageService(String key);

    /**
     * 获取所有存储节点的健康状态
     *
     * @return 存储健康信息列表
     */
    List<StorageHealthInfo> getStorageHealthList();

    /**
     * 手动切换到指定的存储节点
     *
     * @param key 存储节点标识
     * @return 是否切换成功
     */
    boolean switchStorage(String key);

    /**
     * 设置存储策略
     *
     * @param strategy 存储策略枚举
     * @return 是否设置成功
     */
    boolean setStorageStrategy(StorageStrategyEnum strategy);

    /**
     * 获取当前存储策略
     *
     * @return 当前存储策略
     */
    StorageStrategyEnum getStorageStrategy();

    /**
     * 获取当前使用的存储节点标识
     *
     * @return 存储节点标识
     */
    String getCurrentStorageKey();
}
