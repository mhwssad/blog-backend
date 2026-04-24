package com.cybzacg.blogbackend.module.content.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionPageQuery;

import java.util.List;

/**
 * 收藏 Repository。<p>封装用户收藏记录的持久化操作，提供按用户、收藏夹、目标等多维度查询与删除。
 */
public interface SysCollectionRepository extends IService<SysCollection> {
    /**
     * 按管理端查询条件分页查询收藏记录。
     *
     * @param query 查询条件
     * @return 收藏分页结果
     */
    Page<SysCollection> pageByAdminConditions(CollectionPageQuery query);

    /**
     * 按用户分页查询收藏记录。
     *
     * @param userId 用户 ID
     * @param current 页码
     * @param size 每页条数
     * @return 收藏分页结果
     */
    Page<SysCollection> pageByUserId(Long userId, long current, long size);

    /**
     * 查询指定收藏夹下的所有收藏记录。
     *
     * @param folderId 收藏夹 ID
     * @return 收藏记录列表
     */
    List<SysCollection> findByFolderId(Long folderId);

    /**
     * 删除指定收藏夹下的所有收藏记录。
     *
     * @param folderId 收藏夹 ID
     * @return 是否删除成功
     */
    boolean removeByFolderId(Long folderId);

    /**
     * 判断用户是否已在指定收藏夹收藏目标对象。
     *
     * @param userId 用户 ID
     * @param folderId 收藏夹 ID
     * @param targetId 目标 ID
     * @param targetType 目标类型
     * @return 是否已存在收藏
     */
    boolean existsByUserIdAndFolderIdAndTargetIdAndTargetType(Long userId,
                                                              Long folderId,
                                                              Long targetId,
                                                              String targetType);

    /**
     * 查询指定目标类型和目标 ID 的全部收藏记录。
     *
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @return 收藏记录列表
     */
    List<SysCollection> listByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 删除指定目标类型和目标 ID 的全部收藏记录。
     *
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @return 是否删除成功
     */
    boolean removeByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 统计指定收藏夹下的收藏记录数。
     *
     * @param folderId 收藏夹 ID
     * @return 收藏记录数
     */
    long countByFolderId(Long folderId);

    /**
     * 判断用户是否已收藏指定目标对象（不限收藏夹）。
     *
     * @param userId 用户 ID
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @return 是否已收藏
     */
    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
}
