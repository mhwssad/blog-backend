package com.cybzacg.blogbackend.module.file.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.file.FileInfo;
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
     * @return 文件信息，不存在则返回 null
     */
    FileInfo findByMd5AndStatus(String md5, Integer status);

    /**
     * 按 MD5 查询文件。
     *
     * @param md5 文件 MD5
     * @return 文件信息，不存在则返回 null
     */
    FileInfo findByMd5(String md5);

    /**
     * 按状态和关键字查询文件 ID 集合。
     *
     * @param status  文件状态，传入 null 则不以此作为过滤条件
     * @param keyword 关键字，传入 null 或空则不以此作为过滤条件
     * @return 符合条件的文件 ID 有序 Set
     */
    Set<Long> findIdsByStatusAndKeyword(Integer status, String keyword);

    /**
     * 按管理端条件分页查询文件。
     *
     * @param query 查询条件
     * @return 文件分页结果，按更新时间和管理员 ID 倒序排列
     */
    Page<FileInfo> pageAdminFiles(FileAdminPageQuery query);

    /**
     * 重算文件引用元数据：通过子查询实时更新 reference_count 字段，
     * 保证与 file_business_info 表的记录数一致。可选同步提升文件为公开状态。
     *
     * @param fileId        文件 ID
     * @param promotePublic 是否同时将文件标记为公开状态
     * @return 是否更新成功
     */
    boolean refreshReferenceMetadata(Long fileId, boolean promotePublic);

    /**
     * 在无引用时将文件标记为已删除。
     *
     * @param fileId 文件 ID
     * @return 是否更新成功（若存在引用则不执行更新，返回 false）
     */
    boolean markDeletedIfNoReferences(Long fileId);

    /**
     * 按 fileUrl 批量查询文件信息。
     *
     * @param fileUrls 文件 URL 集合
     * @return 匹配的文件列表，若为空集合则直接返回空列表
     */
    List<FileInfo> listByFileUrls(Collection<String> fileUrls);

    /**
     * 按状态批量查询文件，用于定时任务分批处理。
     *
     * @param status 文件状态
     * @param limit  批次大小
     * @return 符合条件的文件列表，最多返回 limit 条
     */
    List<FileInfo> listByStatus(Integer status, int limit);
}
