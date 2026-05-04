package com.cybzacg.blogbackend.module.forum.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "论坛帖子收藏请求")
public class ForumPostCollectRequest {
    @Schema(description = "收藏夹ID，不传则使用默认论坛收藏夹")
    private Long folderId;

    @Schema(description = "收藏备注")
    private String remark;
}
