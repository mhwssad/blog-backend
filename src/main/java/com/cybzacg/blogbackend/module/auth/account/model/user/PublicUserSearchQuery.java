package com.cybzacg.blogbackend.module.auth.account.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公开用户搜索分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户搜索分页查询")
public class PublicUserSearchQuery extends PageQuery {
    @Schema(description = "搜索关键词")
    private String keyword;
}
