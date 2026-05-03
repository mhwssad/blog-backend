package com.cybzacg.blogbackend.module.file.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.mapper.file.FileInfoMapper;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminPageQuery;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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
     * 根据文件MD5和状态查询单条记录，用于秒传判断和已有文件定位。
     *
     * @param md5   文件MD5标识
     * @param status 文件状态
     * @return 匹配的文件记录，若不存在则返回 null
     */
    @Override
    public FileInfo findByMd5AndStatus(String md5, Integer status) {
        return getOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getMd5, md5)
                .eq(FileInfo::getStatus, status)
                .last("limit 1"));
    }

    /**
     * 根据文件MD5查询单条记录，不限制状态，用于通用查重。
     *
     * @param md5 文件MD5标识
     * @return 匹配的文件记录，若不存在则返回 null
     */
    @Override
    public FileInfo findByMd5(String md5) {
        return getOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getMd5, md5)
                .last("limit 1"));
    }

    /**
     * 根据状态和关键词模糊搜索文件ID集合，返回去重的ID有序Set。
     * 关键词同时匹配原始文件名（originalName）和存储文件名（fileName）。
     *
     * @param status  文件状态，传 null 则不以此作为过滤条件
     * @param keyword 关键词，传 null 或空则不以此作为过滤条件
     * @return 符合条件的文件ID有序Set（LinkedHashSet 保证去重且保持插入顺序）
     */
    @Override
    public Set<Long> findIdsByStatusAndKeyword(Integer status, String keyword) {
        return list(new LambdaQueryWrapper<FileInfo>()
                .select(FileInfo::getId)
                .eq(status != null, FileInfo::getStatus, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper.like(FileInfo::getOriginalName, keyword)
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
     * @return 符合条件的文件分页结果，按更新时间和管理员ID倒序排列
     */
    @Override
    public Page<FileInfo> pageAdminFiles(FileAdminPageQuery query) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .eq(query.getUploadUserId() != null, FileInfo::getUploadUserId, query.getUploadUserId())
                .eq(query.getStatus() != null, FileInfo::getStatus, query.getStatus())
                .eq(query.getIsPublic() != null, FileInfo::getIsPublic, query.getIsPublic())
                .eq(StringUtils.hasText(query.getCategory()), FileInfo::getCategory, FileCategoryEnum.normalize(query.getCategory()))
                .and(StringUtils.hasText(query.getKeyword()), w -> w.like(FileInfo::getOriginalName, query.getKeyword())
                        .or()
                        .like(FileInfo::getFileName, query.getKeyword()))
                .orderByDesc(FileInfo::getUpdatedAt)
                .orderByDesc(FileInfo::getId);
        // 按引用类型反查：仅保留在 file_business_info 中存在对应引用类型的文件
        if (StringUtils.hasText(query.getReferenceType())) {
            String type = FileReferenceTypeEnum.normalize(query.getReferenceType());
            wrapper.inSql(FileInfo::getId, "select distinct file_id from file_business_info where reference_type='" + type + "'");
        }
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    /**
     * 刷新指定文件的引用元数据：通过子查询实时更新 reference_count 字段，
     * 保证与 file_business_info 表的记录数一致。同时可选将文件提升为公开状态。
     *
     * @param fileId        文件ID
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
     * 若指定文件在 file_business_info 中已无任何引用，则将其引用计数置为 0 并标记为已删除状态。
     * 用于文件清理场景，确保孤立文件被正确标记。
     *
     * @param fileId 文件ID
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
     * 根据文件URL集合批量查询文件记录。
     *
     * @param fileUrls 文件URL集合
     * @return 匹配的文件记录列表，若为空集合则直接返回空列表
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
     * 根据状态查询文件列表，限制返回条数，用于批量处理等场景。
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
