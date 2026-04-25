package com.cybzacg.blogbackend.module.file.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.module.file.model.user.UserFilePageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 文件业务引用 Repository。<p>封装文件与业务对象关联引用的持久化操作，提供按文件、用户、引用类型等多维度查询与删除。
 */
public interface FileBusinessInfoRepository extends IService<FileBusinessInfo> {
    /**
     * 按文件、用户和业务引用维度查询单条引用。
     *
     * @param fileId        文件 ID
     * @param userId        用户 ID
     * @param referenceType 引用类型
     * @param referenceId   引用目标 ID
     * @return 业务引用
     */
    FileBusinessInfo findByFileUserReference(Long fileId, Long userId, String referenceType, Long referenceId);

    /**
     * 按文件、用户和业务引用维度查询最新引用。
     *
     * @param fileId        文件 ID
     * @param userId        用户 ID
     * @param referenceType 引用类型
     * @param referenceId   引用目标 ID
     * @return 最新业务引用
     */
    FileBusinessInfo findLatestByFileUserReference(Long fileId, Long userId, String referenceType, Long referenceId);

    /**
     * 按用户和过滤条件分页查询文件引用。
     *
     * @param userId  用户 ID
     * @param query   查询条件
     * @param fileIds 物理文件筛选集合
     * @return 文件引用分页
     */
    Page<FileBusinessInfo> pageByUserAndFilters(Long userId, UserFilePageQuery query, Collection<Long> fileIds);

    /**
     * 查询文件关联的所有业务引用。
     *
     * @param fileId 文件 ID
     * @return 业务引用列表
     */
    List<FileBusinessInfo> listByFileId(Long fileId);

    /**
     * 按引用类型和引用目标查询业务引用。
     *
     * @param referenceType 引用类型
     * @param referenceId   引用目标 ID
     * @return 业务引用列表
     */
    List<FileBusinessInfo> listByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    /**
     * 统计文件业务引用数量。
     *
     * @param fileId 文件 ID
     * @return 引用数量
     */
    long countByFileId(Long fileId);

    /**
     * 删除文件关联的所有业务引用。
     *
     * @param fileId 文件 ID
     * @return 是否删除成功
     */
    boolean deleteByFileId(Long fileId);
}
