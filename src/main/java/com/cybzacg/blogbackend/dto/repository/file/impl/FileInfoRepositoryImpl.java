package com.cybzacg.blogbackend.dto.repository.file.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.dto.mapper.file.FileInfoMapper;
import com.cybzacg.blogbackend.dto.repository.file.FileInfoRepository;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminPageQuery;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件物理信息 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供文件物理信息的增删改查。
 */
@Repository
public class FileInfoRepositoryImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoRepository {
    /**
     * 根据 MD5 和状态查询文件，用于秒传判断和已有文件定位。
     *
     * @param md5    文件 MD5
     * @param status 文件状态
     * @return 文件信息，不存在则返回 null
     */
    @Override
    public FileInfo findByMd5AndStatus(String md5, Integer status) {
        return getOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getMd5, md5)
                .eq(FileInfo::getStatus, status)
                .last("limit 1"));
    }

    /**
     * 根据 MD5 查询文件，不限制状态，用于通用查重。
     *
     * @param md5 文件 MD5
     * @return 文件信息，不存在则返回 null
     */
    @Override
    public FileInfo findByMd5(String md5) {
        return getOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getMd5, md5)
                .last("limit 1"));
    }

    /**
     * 根据状态和关键词搜索文件 ID 集合。关键词同时匹配原始文件名和存储文件名。
     *
     * @param status  文件状态，传入 null 则不以此作为过滤条件
     * @param keyword 关键字，传入 null 或空则不以此作为过滤条件
     * @return 符合条件的文件 ID 有序 Set
     */
    @Override
    public Set<Long> findIdsByStatusAndKeyword(Integer status, String keyword) {
        return list(new LambdaQueryWrapper<FileInfo>()
                .select(FileInfo::getId)
                .eq(status != null, FileInfo::getStatus, status)
                .and(StrUtils.hasText(keyword), wrapper -> wrapper.like(FileInfo::getOriginalName, keyword)
                        .or()
                        .like(FileInfo::getFileName, keyword)))
                .stream()
                .map(FileInfo::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 管理员分页查询文件列表，支持按上传用户、状态、公开状态、分类和关键词过滤。
     * 当指定引用类型时，仅返回在 file_business_info 中存在该引用类型的文件。
     *
     * @param query 分页查询条件
     * @return 文件分页结果，按更新时间和管理员 ID 倒序排列
     */
    @Override
    public Page<FileInfo> pageAdminFiles(FileAdminPageQuery query) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .eq(query.getUploadUserId() != null, FileInfo::getUploadUserId, query.getUploadUserId())
                .eq(query.getStatus() != null, FileInfo::getStatus, query.getStatus())
                .eq(query.getIsPublic() != null, FileInfo::getIsPublic, query.getIsPublic())
                .eq(StrUtils.hasText(query.getCategory()), FileInfo::getCategory, FileCategoryEnum.normalize(query.getCategory()))
                .and(StrUtils.hasText(query.getKeyword()), w -> w.like(FileInfo::getOriginalName, query.getKeyword())
                        .or()
                        .like(FileInfo::getFileName, query.getKeyword()))
                .orderByDesc(FileInfo::getUpdatedAt)
                .orderByDesc(FileInfo::getId);
        // 按引用类型反查：仅保留在 file_business_info 中存在对应引用类型的文件
        if (StrUtils.hasText(query.getReferenceType())) {
            String type = FileReferenceTypeEnum.normalize(query.getReferenceType());
            wrapper.inSql(FileInfo::getId, "select distinct file_id from file_business_info where reference_type='" + type + "'");
        }
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    /**
     * 刷新文件引用元数据：通过子查询实时更新 reference_count 字段，
     * 保证与 file_business_info 表的记录数一致。可选同步提升文件为公开状态。
     *
     * @param fileId        文件 ID
     * @param promotePublic 是否同时将文件标记为公开状态
     * @return 是否更新成功
     */
    @Override
    public boolean refreshReferenceMetadata(Long fileId, boolean promotePublic) {
        // 通过子查询实时统计引用数，保证与 file_business_info 表一致
        LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<FileInfo>()
                .eq(FileInfo::getId, fileId)
                .setSql("reference_count = (select count(*) from file_business_info where file_id = " + fileId + ")");
        if (promotePublic) {
            updateWrapper.set(FileInfo::getIsPublic, 1);
        }
        return update(updateWrapper);
    }

    /**
     * 在无引用时将文件标记为已删除。
     * 若指定文件在 file_business_info 中已无任何引用，则将其引用计数置为 0 并标记为已删除状态。
     *
     * @param fileId 文件 ID
     * @return 是否更新成功（若存在引用则不执行更新，返回 false）
     */
    @Override
    public boolean markDeletedIfNoReferences(Long fileId) {
        return update(new LambdaUpdateWrapper<FileInfo>()
                .eq(FileInfo::getId, fileId)
                .apply("not exists (select 1 from file_business_info where file_id = {0})", fileId)
                .set(FileInfo::getReferenceCount, 0)
                .set(FileInfo::getStatus, FileStatusEnum.DELETED.getValue()));
    }

    /**
     * 根据文件 URL 批量查询文件。
     *
     * @param fileUrls 文件 URL 集合
     * @return 匹配的文件列表，若为空集合则直接返回空列表
     */
    @Override
    public List<FileInfo> listByFileUrls(Collection<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<FileInfo>()
                .in(FileInfo::getFileUrl, fileUrls));
    }

    /**
     * 根据状态查询文件列表，用于定时任务分批处理。
     *
     * @param status 文件状态
     * @param limit  最大返回条数
     * @return 符合条件的文件列表，最多返回 limit 条
     */
    @Override
    public List<FileInfo> listByStatus(Integer status, int limit) {
        return list(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getStatus, status)
                .last("limit " + limit));
    }
}
