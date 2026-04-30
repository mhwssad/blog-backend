package com.cybzacg.blogbackend.module.chat.conversation.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台会话分页查询条件。
 */
@Data
@Schema(description = "后台会话分页查询条件")
public class ChatAdminConversationPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;

    @Schema(description = "关键字，当前支持按会话名称和最后一条消息内容筛选")
    private String keyword;

    @Schema(description = "会话类型：single/group/global")
    private String conversationType;

    @Schema(description = "会话状态：0-禁用，1-正常，2-已解散")
    private Integer status;

    @Schema(description = "群主ID")
    private Long ownerId;

    @Schema(description = "成员用户ID，仅筛选包含该用户的会话")
    private Long memberUserId;

    @Schema(description = "是否全站会话：0-否，1-是")
    private Integer isAllSite;
}
