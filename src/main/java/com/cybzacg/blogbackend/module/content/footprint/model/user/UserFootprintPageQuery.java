package com.cybzacg.blogbackend.module.content.footprint.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户足迹分页查询条件")
public class UserFootprintPageQuery extends PageQuery {
    @Schema(description = "目标类型")
    private String targetType;
}
