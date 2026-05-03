package com.cybzacg.blogbackend.module.follow.model.publics;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公开关注列表分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公开关注列表分页查询条件")
public class PublicFollowPageQuery extends PageQuery {

}
