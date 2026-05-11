package com.cybzacg.blogbackend.dto.mapper.migration;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * BlogMigrationTask Mapper。
 */
@Mapper
public interface BlogMigrationTaskMapper
    extends BaseMapper<BlogMigrationTask> {}
