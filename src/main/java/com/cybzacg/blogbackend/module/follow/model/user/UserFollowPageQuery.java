package com.cybzacg.blogbackend.module.follow.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关注列表分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "关注列表分页查询条件")
public class UserFollowPageQuery extends PageQuery {

    @Schema(description = "是否只看特别关注")
    private Boolean specialOnly;
}
