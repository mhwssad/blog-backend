package com.cybzacg.blogbackend.module.follow.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 粉丝列表分页查询条件。
 */
@Data
@Schema(description = "粉丝列表分页查询条件")
public class UserFanPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;
}
