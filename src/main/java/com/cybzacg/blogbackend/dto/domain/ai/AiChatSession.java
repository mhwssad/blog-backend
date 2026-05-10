package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话会话表。
 */
@Data
@TableName("ai_chat_session")
public class AiChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 渠道配置ID */
    private Long channelConfigId;
    /** 会话标题 */
    private String title;
    /** 会话场景：general/article/chat/profile */
    private String sceneType;
    /** 状态：0-关闭，1-正常 */
    private Integer status;
    /** 最后消息时间 */
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
