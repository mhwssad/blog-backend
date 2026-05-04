package com.cybzacg.blogbackend.module.auth.notice.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "后台通知信息")
public class SysNoticeAdminVO {
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
    @Schema(description = "目标类型")
    private Integer targetType;
    @Schema(description = "目标用户ID列表")
    private List<Long> targetUserIds;
    @Schema(description = "发布人ID")
    private Long publisherId;
    @Schema(description = "发布状态")
    private Integer publishStatus;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "撤回时间")
    private LocalDateTime revokeTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "业务目标类型")
    private String businessType;
    @Schema(description = "业务目标ID")
    private Long businessId;
    @Schema(description = "前端跳转路径")
    private String actionPath;
}
