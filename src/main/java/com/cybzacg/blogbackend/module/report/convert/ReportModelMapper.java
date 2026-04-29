package com.cybzacg.blogbackend.module.report.convert;

import com.cybzacg.blogbackend.domain.SysReportHandleLog;
import com.cybzacg.blogbackend.domain.SysReportRecord;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminVO;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import com.cybzacg.blogbackend.module.report.model.user.ReportCreateRequest;
import com.cybzacg.blogbackend.module.report.model.user.ReportVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 举报模块对象转换器。
 */
@Mapper(componentModel = "spring")
public interface ReportModelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "handlerUserId", ignore = true)
    @Mapping(target = "resultType", ignore = true)
    @Mapping(target = "punishmentType", ignore = true)
    @Mapping(target = "evidenceJson", ignore = true)
    @Mapping(target = "handledAt", ignore = true)
    @Mapping(target = "remark", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "reporterUserId", ignore = true)
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "reportTargetType", source = "targetType")
    @Mapping(target = "reportTargetId", source = "targetId")
    SysReportRecord toRecord(ReportCreateRequest request);

    @Mapping(target = "reporterUsername", ignore = true)
    @Mapping(target = "handlerUsername", ignore = true)
    ReportAdminVO toAdminVO(SysReportRecord record);

    ReportVO toUserVO(SysReportRecord record);

    @Mapping(target = "operatorUsername", ignore = true)
    ReportHandleLogVO toHandleLogVO(SysReportHandleLog log);
}
