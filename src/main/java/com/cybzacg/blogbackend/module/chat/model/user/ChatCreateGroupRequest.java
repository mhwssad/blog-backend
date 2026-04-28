package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建群聊请求。
 */
@Data
@Schema(description = "创建群聊请求")
public class ChatCreateGroupRequest {
    @NotBlank(message = "群名称不能为空")
    @Size(max = 128, message = "群名称长度不能超过128")
    @Schema(description = "群名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 512, message = "群头像长度不能超过512")
    @Schema(description = "群头像")
    private String avatar;

    @Size(max = 256, message = "群简介长度不能超过256")
    @Schema(description = "群简介")
    private String description;

    @Size(max = 512, message = "群公告长度不能超过512")
    @Schema(description = "群公告")
    private String announcement;

    @Size(max = 32, message = "群分类编码长度不能超过32")
    @Schema(description = "群分类编码")
    private String categoryCode;

    @Schema(description = "可见范围：public/private，默认 private")
    private String visibilityScope;

    @Schema(description = "加入规则：free/approval/invite_only，默认 free")
    private String joinRule;

    @Min(value = 1, message = "发言最低等级不能小于1")
    @Schema(description = "发言最低等级，默认1")
    private Integer speakLevelLimit;

    @Min(value = 0, message = "成员上限不能小于0")
    @Schema(description = "成员上限，0表示不限制")
    private Integer memberLimit;

    @NotEmpty(message = "群成员不能为空")
    @Schema(description = "初始成员用户ID列表，不需要包含自己", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> memberUserIds;
}
