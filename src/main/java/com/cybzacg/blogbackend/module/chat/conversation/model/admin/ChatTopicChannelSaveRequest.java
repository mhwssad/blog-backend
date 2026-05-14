package com.cybzacg.blogbackend.module.chat.conversation.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台主题频道保存请求。
 */
@Data
@Schema(description = "后台主题频道保存请求")
public class ChatTopicChannelSaveRequest {
    @NotBlank(message = "频道名称不能为空")
    @Size(max = 128, message = "频道名称长度不能超过128")
    @Schema(description = "频道名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 512, message = "频道头像长度不能超过512")
    @Schema(description = "频道头像")
    private String avatar;

    @Size(max = 256, message = "频道简介长度不能超过256")
    @Schema(description = "频道简介")
    private String description;

    @Size(max = 512, message = "频道公告长度不能超过512")
    @Schema(description = "频道公告")
    private String announcement;

    @Size(max = 32, message = "频道分类编码长度不能超过32")
    @Schema(description = "频道分类编码")
    private String categoryCode;

    @Pattern(regexp = "public|member|private", message = "频道可见范围不合法")
    @Schema(description = "可见范围：public/member/private，默认 member")
    private String visibilityScope;

    @Pattern(regexp = "free|approval|invite_only", message = "频道加入规则不合法")
    @Schema(description = "加入规则：free/approval/invite_only，默认 approval")
    private String joinRule;

    @Min(value = 1, message = "发言最低等级不能小于1")
    @Schema(description = "发言最低等级，默认1")
    private Integer speakLevelLimit;

    @Min(value = 0, message = "成员上限不能小于0")
    @Schema(description = "成员上限，0表示不限制")
    private Integer memberLimit;

    @Min(value = 0, message = "慢速模式秒数不能小于0")
    @Schema(description = "慢速模式秒数，0表示关闭")
    private Integer slowModeSeconds;

    @Schema(description = "展示排序")
    private Integer displaySort;

    @Schema(description = "频道负责人用户ID，不传则创建时使用当前管理员")
    private Long ownerId;
}
