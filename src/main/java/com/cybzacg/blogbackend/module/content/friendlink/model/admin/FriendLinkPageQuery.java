package com.cybzacg.blogbackend.module.content.friendlink.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "友情链接分页查询")
public class FriendLinkPageQuery extends PageQuery {
    @Schema(description = "站点名称（模糊）")
    private String name;
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
}
