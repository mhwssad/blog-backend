package com.cybzacg.blogbackend.module.chat.member.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交频道创建申请请求。
 */
@Data
@Schema(description = "用户提交频道创建申请请求")
public class ChatChannelApplicationSubmitRequest {
    @NotBlank(message = "频道名称不能为空")
    @Size(max = 128, message = "频道名称长度不能超过128")
    @Schema(description = "期望频道名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String desiredName;

    @Size(max = 32, message = "频道场景长度不能超过32")
    @Schema(description = "期望频道场景，当前默认 topic_channel")
    private String desiredSceneType;

    @Size(max = 32, message = "频道分类编码长度不能超过32")
    @Schema(description = "期望频道分类编码")
    private String desiredCategoryCode;

    @Size(max = 1024, message = "申请说明长度不能超过1024")
    @Schema(description = "申请说明")
    private String description;
}
