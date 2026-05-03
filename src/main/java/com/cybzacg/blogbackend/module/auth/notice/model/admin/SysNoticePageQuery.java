package com.cybzacg.blogbackend.module.auth.notice.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通知分页查询条件")
public class SysNoticePageQuery extends PageQuery {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "发布状态")
    private Integer publishStatus;

    @Schema(description = "目标类型")
    private Integer targetType;
}
