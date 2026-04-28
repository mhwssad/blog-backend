package com.cybzacg.blogbackend.module.ai.model.data;

import lombok.Data;

/**
 * AI模型调用结果。
 */
@Data
public class AiModelCallResult {
    /** 响应内容 */
    private String content;
    /** 请求token数 */
    private int requestTokens;
    /** 响应token数 */
    private int responseTokens;
    /** 总token数 */
    private int totalTokens;
    /** 是否成功 */
    private boolean success;
    /** 错误信息 */
    private String errorMessage;
}
