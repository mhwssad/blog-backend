package com.cybzacg.blogbackend.module.forum.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户侧论坛帖子分页查询条件")
public class UserForumPostPageQuery extends PageQuery {
    @Schema(description = "搜索关键字")
    private String keyword;

    @Schema(description = "版块ID")
    private Long sectionId;

    @Schema(description = "帖子状态")
    private Integer status;
}
