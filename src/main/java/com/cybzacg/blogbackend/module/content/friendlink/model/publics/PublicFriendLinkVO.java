package com.cybzacg.blogbackend.module.content.friendlink.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "友情链接公开 VO")
public class PublicFriendLinkVO {
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
}
