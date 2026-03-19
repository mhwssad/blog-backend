package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "我的通知分页查询条件")
public class UserNoticePageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "读取状态 0-未读 1-已读")
    private Integer isRead;
}
