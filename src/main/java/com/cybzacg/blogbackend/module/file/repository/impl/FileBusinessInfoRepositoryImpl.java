package com.cybzacg.blogbackend.module.file.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.mapper.file.FileBusinessInfoMapper;
import com.cybzacg.blogbackend.module.file.model.user.UserFilePageQuery;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 文件业务引用 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供文件与业务对象关联引用的增删改查。
 */
@Repository
public class FileBusinessInfoRepositoryImpl extends ServiceImpl<FileBusinessInfoMapper, FileBusinessInfo>
        implements FileBusinessInfoRepository {
    /**
     * 根据文件ID、用户ID、引用类型和引用ID查询单条业务引用记录。
     *
     * @param fileId       文件ID
     * @param userId       用户ID
     * @param referenceType 引用类型
     * @param referenceId  引用业务ID
     * @return 匹配的记录，若不存在则返回 null
     */
    @Override
    public FileBusinessInfo findByFileUserReference(Long fileId, Long userId, String referenceType, Long referenceId) {
        return getOne(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId)
                .eq(FileBusinessInfo::getUserId, userId)
                .eq(FileBusinessInfo::getReferenceType, referenceType)
                .eq(FileBusinessInfo::getReferenceId, referenceId)
                .last("limit 1"));
    }

    /**
     * 查询指定文件、用户、引用类型和引用ID对应的最新一条业务引用记录。
     * 与 {@link #findByFileUserReference} 的区别在于：此方法优先按 ID 倒序取最新记录，
     * 且 userId 为可选参数（当为 null 时匹配所有用户）。
     *
     * @param fileId        文件ID
     * @param userId        用户ID，传入 null 则不以此作为过滤条件
     * @param referenceType 引用类型
     * @param referenceId   引用业务ID
     * @return 最新一条匹配的记录，若不存在则返回 null
     */
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

    /**
     * 分页查询用户文件业务引用列表，支持按分类、引用类型和文件ID集合过滤。
     *
     * @param userId  所属用户ID
     * @param query   分页查询条件，包含分类、引用类型等过滤参数
     * @param fileIds 文件ID集合，若不为 null 则结果仅返回属于该集合的文件记录
     * @return 符合条件的分页结果，按创建时间和ID倒序排列
     */
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

    /**
     * 查询指定文件ID对应的所有业务引用记录列表。
     *
     * @param fileId 文件ID
     * @return 该文件的所有业务引用记录，按创建时间和ID倒序排列
     */
    @Override
    public List<FileBusinessInfo> listByFileId(Long fileId) {
        return list(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId)
                .orderByDesc(FileBusinessInfo::getCreatedAt)
                .orderByDesc(FileBusinessInfo::getId));
    }

    /**
     * 查询指定引用类型和引用业务ID对应的所有业务引用记录列表。
     *
     * @param referenceType 引用类型
     * @param referenceId   引用业务ID
     * @return该引用下所有文件业务引用记录，按创建时间和ID倒序排列
     */
    @Override
    public List<FileBusinessInfo> listByReferenceTypeAndReferenceId(String referenceType, Long referenceId) {
        return list(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getReferenceType, referenceType)
                .eq(FileBusinessInfo::getReferenceId, referenceId)
                .orderByDesc(FileBusinessInfo::getCreatedAt)
                .orderByDesc(FileBusinessInfo::getId));
    }

    /**
     * 统计指定文件ID对应的业务引用记录数量。
     *
     * @param fileId 文件ID
     * @return 引用记录数量
     */
    @Override
    public long countByFileId(Long fileId) {
        Long count = count(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId));
        return count == null ? 0L : count;
    }

    /**
     * 删除指定文件ID对应的所有业务引用记录。
     *
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByFileId(Long fileId) {
        return remove(new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getFileId, fileId));
    }
}
