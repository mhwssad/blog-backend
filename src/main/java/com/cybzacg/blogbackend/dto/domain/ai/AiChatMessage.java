package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话消息表。
 */
@Data
@TableName("ai_chat_message")
public class AiChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会话ID */
    private Long sessionId;
    /** 关联用户ID */
    private Long userId;
    /** 角色类型：user/assistant/system */
    private String roleType;
    /** 消息内容 */
    private String content;
    /** 请求场景：general/article/chat/profile */
    private String requestSceneType;
    /** 关联目标ID */
    private Long requestTargetId;
    /** 消息 token 数 */
    private Integer tokenCount;
    /** 当次读取范围快照 JSON */
    private String dataScopeSnapshot;
    /** RAG 引用来源 JSON */
    private String ragReferenceJson;
    /** 响应状态：0-失败，1-成功 */
    private Integer responseStatus;
    /** 错误信息 */
    private String errorMessage;
    private LocalDateTime createdAt;
}
