package com.cybzacg.blogbackend.module.file.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminPageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 文件物理信息 Repository。<p>封装文件物理信息表的持久化操作，提供按 MD5 秒传检测、后台分页查询及引用元数据刷新。
 */
public interface FileInfoRepository extends IService<FileInfo> {
    /**
     * 按 MD5 和状态查询文件。
     *
     * @param md5    文件 MD5
     * @param status 文件状态
     * @return 文件信息
     */
    FileInfo findByMd5AndStatus(String md5, Integer status);

    /**
     * 按 MD5 查询文件。
     *
     * @param md5 文件 MD5
     * @return 文件信息
     */
    FileInfo findByMd5(String md5);

    /**
     * 按状态和关键字查询文件 ID 集合。
     *
     * @param status  文件状态
     * @param keyword 关键字
     * @return 文件 ID 集合
     */
    Set<Long> findIdsByStatusAndKeyword(Integer status, String keyword);

    /**
     * 按管理端条件分页查询文件。
     *
     * @param query 查询条件
     * @return 文件分页结果
     */
    Page<FileInfo> pageAdminFiles(FileAdminPageQuery query);

    /**
     * 重算文件引用元数据。
     *
     * @param fileId        文件 ID
     * @param promotePublic 是否提升公开状态
     * @return 是否更新成功
     */
    boolean refreshReferenceMetadata(Long fileId, boolean promotePublic);

    /**
     * 在无引用时将文件标记为删除。
     *
     * @param fileId 文件 ID
     * @return 是否更新成功
     */
    boolean markDeletedIfNoReferences(Long fileId);

    /**
     * 按 fileUrl 批量查询文件信息。
     */
    List<FileInfo> listByFileUrls(Collection<String> fileUrls);
}
