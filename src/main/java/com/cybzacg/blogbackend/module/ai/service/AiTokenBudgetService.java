package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;

import java.util.List;

/**
 * AI Token 预算服务。
 *
 * <p>负责根据渠道配置的五维预算（input/history/rag/attachment/output）对输入进行预估和裁剪。
 */
public interface AiTokenBudgetService {

    /**
     * 预算校验结果。
     */
    record BudgetCheck(
            boolean withinBudget,
            int inputTokens,
            int historyTokens,
            int ragTokens,
            int attachmentTokens
    ) {}

    /**
     * 校验并预估当前请求的 Token 预算。
     */
    BudgetCheck checkBudget(AiChannelConfig config,
                            String userInput,
                            List<AiChatMessage> historyMessages,
                            String ragContext,
                            List<FileInfo> attachments);

    /**
     * 裁剪历史消息以适应预算限制。
     */
    List<AiChatMessage> trimHistory(List<AiChatMessage> messages, int maxHistoryTokens);

    /**
     * 裁剪 RAG 上下文以适应预算限制。
     */
    String trimRagContext(String ragContext, int maxRagTokens);

    /**
     * 估算文本的 token 数（字符数 / 2，中文场景粗略估算）。
     */
    int estimateTokens(String text);

    /**
     * 估算图片附件的 token 数（按固定值估算）。
     */
    int estimateImageTokens(FileInfo image);
}
