package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 群公告更新请求。
 */
@Data
@Schema(description = "群公告更新请求")
public class ChatGroupNoticeUpdateRequest {
    @Size(max = 500, message = "群公告长度不能超过500")
    @Schema(description = "群公告内容；为空表示清空")
    private String notice;
}
