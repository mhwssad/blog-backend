package com.cybzacg.blogbackend.module.content.collection.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户收藏信息")
public class CollectionVO {
    @Schema(description = "收藏ID")
    private Long id;
    @Schema(description = "收藏夹ID")
    private Long folderId;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "目标标题")
    private String targetTitle;
    @Schema(description = "目标地址")
    private String targetUrl;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
