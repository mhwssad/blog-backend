package com.cybzacg.blogbackend.module.ai.model.data;

import lombok.Data;

/**
 * AI 流式输出事件。
 */
@Data
public class AiStreamEvent {
    /** delta: 增量文本, usage: token 用量, done: 结束标记, error: 错误信息 */
    private String type;
    private String content;
    private Integer requestTokens;
    private Integer responseTokens;
    private Integer totalTokens;

    /**
     * 创建增量文本事件。
     *
     * @param text 增量文本内容
     * @return 增量文本事件
     */
    public static AiStreamEvent delta(String text) {
        AiStreamEvent e = new AiStreamEvent();
        e.setType("delta");
        e.setContent(text);
        return e;
    }

    /**
     * 创建 token 用量事件。
     *
     * @param req   请求 token 数
     * @param resp  响应 token 数
     * @param total 总 token 数
     * @return token 用量事件
     */
    public static AiStreamEvent usage(int req, int resp, int total) {
        AiStreamEvent e = new AiStreamEvent();
        e.setType("usage");
        e.setRequestTokens(req);
        e.setResponseTokens(resp);
        e.setTotalTokens(total);
        return e;
    }

    /**
     * 创建流式输出结束事件。
     *
     * @return 结束事件
     */
    public static AiStreamEvent done() {
        AiStreamEvent e = new AiStreamEvent();
        e.setType("done");
        return e;
    }

    /**
     * 创建错误事件。
     *
     * @param message 错误信息
     * @return 错误事件
     */
    public static AiStreamEvent error(String message) {
        AiStreamEvent e = new AiStreamEvent();
        e.setType("error");
        e.setContent(message);
        return e;
    }
}
