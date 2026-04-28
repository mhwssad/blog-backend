package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊邀请链接表。
 */
@Data
@TableName("chat_group_invite_link")
public class ChatGroupInviteLink {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 群聊会话ID */
    private Long conversationId;
    /** 邀请链接令牌 */
    private String inviteToken;
    /** 创建人ID */
    private Long createdBy;
    /** 过期时间，空表示不过期 */
    private LocalDateTime expireAt;
    /** 最大使用次数，0表示不限制 */
    private Integer maxUseCount;
    /** 已使用次数 */
    private Integer usedCount;
    /** 状态：0-停用，1-启用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
