package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话表。
 */
@Data
@TableName("chat_conversation")
public class ChatConversation {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 会话类型（single-单聊，group-群聊，system-系统）
     */
    private String conversationType;
    /**
     * 会话名称（群聊时由创建者指定）
     */
    private String name;
    /**
     * 会话头像URL
     */
    private String avatar;
    /**
     * 创建者ID（群主）
     */
    private Long ownerId;
    /**
     * 单聊唯一标识（双方用户ID排序拼接的哈希）
     */
    private String singlePairKey;
    /**
     * 是否全站会话：0-否，1-是
     */
    private Integer isAllSite;
    /**
     * 全站会话唯一标识
     */
    private String allSiteKey;
    /**
     * 会话状态：0-正常，1-已解散
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 最新消息ID
     */
    private Long lastMessageId;
    /**
     * 最新消息时间
     */
    private LocalDateTime lastMessageTime;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
