package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 聊天会话表。
 */
@Data
@TableName("chat_conversation")
public class ChatConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationType;
    private String name;
    private String avatar;
    private Long ownerId;
    private String singlePairKey;
    private Integer isAllSite;
    private String allSiteKey;
    private Integer status;
    private String remark;
    private Long lastMessageId;
    private Date lastMessageTime;
    private Date createdAt;
    private Date updatedAt;
}
