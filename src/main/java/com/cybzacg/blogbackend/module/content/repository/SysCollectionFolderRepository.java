package com.cybzacg.blogbackend.module.content.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionPageQuery;

import java.util.List;

/**
 * 收藏夹 Repository。
 */
public interface SysCollectionFolderRepository extends IService<SysCollectionFolder> {
    /**
     * 按管理端查询条件分页查询收藏夹。
     *
     * @param query 查询条件
     * @return 收藏夹分页结果
     */
    Page<SysCollectionFolder> pageByAdminConditions(CollectionPageQuery query);

    /**
     * 按用户分页查询收藏夹，默认夹优先，其次按排序值和 ID 排序。
     *
     * @param userId 用户 ID
     * @param current 页码
     * @param size 每页条数
     * @return 收藏夹分页结果
     */
    Page<SysCollectionFolder> pageByUserIdOrderByDefaultAndSort(Long userId, long current, long size);

    /**
     * 查询用户指定类型的默认收藏夹。
     *
     * @param userId 用户 ID
     * @param folderType 收藏夹类型
     * @return 默认收藏夹
     */
    SysCollectionFolder findDefaultByUserIdAndFolderType(Long userId, String folderType);

    /**
     * 查询用户指定类型的所有默认收藏夹。
     *
     * @param userId 用户 ID
     * @param folderType 收藏夹类型
     * @return 默认收藏夹列表
     */
    List<SysCollectionFolder> findDefaultsByUserIdAndFolderType(Long userId, String folderType);
}
