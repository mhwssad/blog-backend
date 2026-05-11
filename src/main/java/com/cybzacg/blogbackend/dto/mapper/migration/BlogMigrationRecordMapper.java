package com.cybzacg.blogbackend.dto.mapper.migration;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * BlogMigrationRecord Mapper。
 */
@Mapper
public interface BlogMigrationRecordMapper
    extends BaseMapper<BlogMigrationRecord> {}
