package com.cybzacg.blogbackend.module.auth.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户作者申请分页查询条件。
 */
@Data
@Schema(description = "用户作者申请分页查询条件")
public class UserAuthorApplicationPageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
}
