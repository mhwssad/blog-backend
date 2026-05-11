package com.cybzacg.blogbackend.module.follow.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 粉丝列表分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "粉丝列表分页查询条件")
public class UserFanPageQuery extends PageQuery {

}
