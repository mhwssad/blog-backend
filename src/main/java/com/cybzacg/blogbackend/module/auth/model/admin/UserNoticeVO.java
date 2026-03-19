package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "用户通知信息")
public class UserNoticeVO {
    @Schema(description = "通知ID")
    private Long id;
    @Schema(description = "通知标题")
    private String title;
    @Schema(description = "通知内容")
    private String content;
    @Schema(description = "通知类型")
    private Integer type;
    @Schema(description = "通知等级")
    private String level;
    @Schema(description = "发布时间")
    private Date publishTime;
    @Schema(description = "是否已读")
    private Integer isRead;
    @Schema(description = "阅读时间")
    private Date readTime;
}
