package com.cybzacg.blogbackend.module.follow.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公开关注列表分页查询条件。
 */
@Data
@Schema(description = "公开关注列表分页查询条件")
public class PublicFollowPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;
}
