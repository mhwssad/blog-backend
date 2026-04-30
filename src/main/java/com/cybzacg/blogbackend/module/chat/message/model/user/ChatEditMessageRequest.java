package com.cybzacg.blogbackend.module.chat.message.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑消息请求。
 */
@Data
@Schema(description = "编辑消息请求")
public class ChatEditMessageRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容长度不能超过2000")
    @Schema(description = "新的文本消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
