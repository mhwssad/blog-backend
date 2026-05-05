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

    public static AiStreamEvent delta(String text) {
        AiStreamEvent e = new AiStreamEvent();
        e.setType("delta");
        e.setContent(text);
        return e;
    }

    public static AiStreamEvent usage(int req, int resp, int total) {
        AiStreamEvent e = new AiStreamEvent();
        e.setType("usage");
        e.setRequestTokens(req);
        e.setResponseTokens(resp);
        e.setTotalTokens(total);
        return e;
    }

    public static AiStreamEvent done() {
        AiStreamEvent e = new AiStreamEvent();
        e.setType("done");
        return e;
    }

    public static AiStreamEvent error(String message) {
        AiStreamEvent e = new AiStreamEvent();
        e.setType("error");
        e.setContent(message);
        return e;
    }
}
