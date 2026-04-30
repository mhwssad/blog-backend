package com.cybzacg.blogbackend.domain.ai;

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
    private LocalDateTime createdAt;
}
