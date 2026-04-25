package com.cybzacg.blogbackend.module.file.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.mapper.FileInfoMapper;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminPageQuery;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件物理信息 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供文件物理信息的增删改查。
 */
@Repository
public class FileInfoRepositoryImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoRepository {
    /**
     * {@inheritDoc}
     */
    @Override
    public FileInfo findByMd5AndStatus(String md5, Integer status) {
        return getOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getMd5, md5)
                .eq(FileInfo::getStatus, status)
                .last("limit 1"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileInfo findByMd5(String md5) {
        return getOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getMd5, md5)
                .last("limit 1"));
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public boolean markDeletedIfNoReferences(Long fileId) {
        return update(new LambdaUpdateWrapper<FileInfo>()
                .eq(FileInfo::getId, fileId)
                .apply("not exists (select 1 from file_business_info where file_id = {0})", fileId)
                .set(FileInfo::getReferenceCount, 0)
                .set(FileInfo::getStatus, FileStatusEnum.DELETED.getValue()));
    }
}
