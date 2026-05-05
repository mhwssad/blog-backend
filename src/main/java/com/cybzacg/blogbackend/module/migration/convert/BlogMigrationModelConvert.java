package com.cybzacg.blogbackend.module.migration.convert;

import com.cybzacg.blogbackend.domain.migration.BlogMigrationRecord;
import com.cybzacg.blogbackend.domain.migration.BlogMigrationTask;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationRecordVO;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationTaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 博客迁移对象转换。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BlogMigrationModelConvert {
    BlogMigrationTaskVO toTaskVO(BlogMigrationTask task);

    BlogMigrationRecordVO toRecordVO(BlogMigrationRecord record);
}
