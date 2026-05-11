package com.cybzacg.blogbackend.module.report.convert;

import com.cybzacg.blogbackend.dto.domain.report.SysReportHandleLog;
import com.cybzacg.blogbackend.dto.domain.report.SysReportRecord;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminVO;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import com.cybzacg.blogbackend.module.report.model.user.ReportCreateRequest;
import com.cybzacg.blogbackend.module.report.model.user.ReportVO;
import org.mapstruct.*;

/**
 * 举报模块对象转换器。
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReportModelConvert {

    @Mapping(target = "reportTargetType", source = "targetType")
    @Mapping(target = "reportTargetId", source = "targetId")
    SysReportRecord toRecord(ReportCreateRequest request);

    ReportAdminVO toAdminVO(SysReportRecord record);

    ReportVO toUserVO(SysReportRecord record);

    ReportHandleLogVO toHandleLogVO(SysReportHandleLog log);
}
