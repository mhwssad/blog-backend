package com.cybzacg.blogbackend.core.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基类，所有 *PageQuery 类应继承此类
 */
@Data
public abstract class PageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
}