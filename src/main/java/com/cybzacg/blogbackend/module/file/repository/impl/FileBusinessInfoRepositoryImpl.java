package com.cybzacg.blogbackend.module.file.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.mapper.FileBusinessInfoMapper;
import com.cybzacg.blogbackend.module.file.model.user.UserFilePageQuery;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 文件业务引用 Repository 实现。
 */
@Repository
public class FileBusinessInfoRepositoryImpl extends ServiceImpl<FileBusinessInfoMapper, FileBusinessInfo>
        implements FileBusinessInfoRepository {
    @Override
    public FileBusinessInfo findByFileUserReference(Long fileId, Long userId, String referenceType, Long referenceId) {
        return getOne(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId)
                .eq(FileBusinessInfo::getUserId, userId)
                .eq(FileBusinessInfo::getReferenceType, referenceType)
                .eq(FileBusinessInfo::getReferenceId, referenceId)
                .last("limit 1"));
    }

    @Override
    public FileBusinessInfo findLatestByFileUserReference(Long fileId, Long userId, String referenceType, Long referenceId) {
        LambdaQueryWrapper<FileBusinessInfo> wrapper = new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId)
                .eq(FileBusinessInfo::getReferenceType, referenceType)
                .eq(FileBusinessInfo::getReferenceId, referenceId)
                .orderByDesc(FileBusinessInfo::getId)
                .last("limit 1");
        if (userId != null) {
            wrapper.eq(FileBusinessInfo::getUserId, userId);
        }
        return getOne(wrapper);
    }

    @Override
    public Page<FileBusinessInfo> pageByUserAndFilters(Long userId, UserFilePageQuery query, Collection<Long> fileIds) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getUserId, userId)
                .eq(StringUtils.hasText(query.getCategory()), FileBusinessInfo::getCategory, FileCategoryEnum.normalize(query.getCategory()))
                .eq(StringUtils.hasText(query.getReferenceType()), FileBusinessInfo::getReferenceType, FileReferenceTypeEnum.normalize(query.getReferenceType()))
                .in(fileIds != null, FileBusinessInfo::getFileId, fileIds)
                .orderByDesc(FileBusinessInfo::getCreatedAt)
                .orderByDesc(FileBusinessInfo::getId));
    }

    @Override
    public List<FileBusinessInfo> listByFileId(Long fileId) {
        return list(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId)
                .orderByDesc(FileBusinessInfo::getCreatedAt)
                .orderByDesc(FileBusinessInfo::getId));
    }

    @Override
    public List<FileBusinessInfo> listByReferenceTypeAndReferenceId(String referenceType, Long referenceId) {
        return list(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getReferenceType, referenceType)
                .eq(FileBusinessInfo::getReferenceId, referenceId)
                .orderByDesc(FileBusinessInfo::getCreatedAt)
                .orderByDesc(FileBusinessInfo::getId));
    }

    @Override
    public long countByFileId(Long fileId) {
        Long count = count(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId));
        return count == null ? 0L : count;
    }

    @Override
    public boolean deleteByFileId(Long fileId) {
        return remove(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId));
    }
}
