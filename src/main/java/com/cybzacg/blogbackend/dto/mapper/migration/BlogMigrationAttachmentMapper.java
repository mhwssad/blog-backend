package com.cybzacg.blogbackend.dto.mapper.migration;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * BlogMigrationAttachment Mapper。
 */
@Mapper
public interface BlogMigrationAttachmentMapper
    extends BaseMapper<BlogMigrationAttachment> {}
