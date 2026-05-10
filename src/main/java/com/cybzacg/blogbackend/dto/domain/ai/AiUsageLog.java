package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 使用日志表。
 */
@Data
@TableName("ai_usage_log")
public class AiUsageLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 渠道配置ID */
    private Long channelConfigId;
    /** 会话ID */
    private Long sessionId;
    /** 请求场景：general/article/chat/profile */
    private String requestSceneType;
    /** 请求 token 数 */
    private Integer requestTokens;
    /** 响应 token 数 */
    private Integer responseTokens;
    /** 总 token 数 */
    private Integer totalTokens;
    /** 额度消耗 */
    private Integer quotaCost;
    /** 成功状态：0-失败，1-成功 */
    private Integer successStatus;
    /** 错误码 */
    private String errorCode;
    /** 是否启用 RAG：0-否，1-是 */
    private Integer ragEnabled;
    /** RAG 命中数量 */
    private Integer ragHitCount;
    /** RAG 检索耗时毫秒 */
    private Long ragDurationMs;
    /** RAG 引用来源 JSON */
    private String ragReferenceJson;
    private LocalDateTime createdAt;
}
