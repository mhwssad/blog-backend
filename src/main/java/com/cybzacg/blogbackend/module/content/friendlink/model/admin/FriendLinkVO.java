package com.cybzacg.blogbackend.module.content.friendlink.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "友情链接后台 VO")
public class FriendLinkVO {
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "站点名称")
    private String name;
    @Schema(description = "站点地址")
    private String url;
    @Schema(description = "图标地址")
    private String iconUrl;
    @Schema(description = "站点描述")
    private String description;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
