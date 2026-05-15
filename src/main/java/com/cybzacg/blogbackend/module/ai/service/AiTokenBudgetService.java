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
     *
     * @param config          渠道配置（含五维预算上限）
     * @param userInput       用户输入文本
     * @param historyMessages 上下文历史消息列表
     * @param ragContext      RAG 检索上下文文本
     * @param attachments     附件列表
     * @return 预算校验结果
     */
    BudgetCheck checkBudget(AiChannelConfig config,
                            String userInput,
                            List<AiChatMessage> historyMessages,
                            String ragContext,
                            List<FileInfo> attachments);

    /**
     * 裁剪历史消息以适应预算限制。
     *
     * @param messages          原始历史消息列表
     * @param maxHistoryTokens 历史消息最大 token 数
     * @return 裁剪后的历史消息列表
     */
    List<AiChatMessage> trimHistory(List<AiChatMessage> messages, int maxHistoryTokens);

    /**
     * 裁剪 RAG 上下文以适应预算限制。
     *
     * @param ragContext    原始 RAG 上下文文本
     * @param maxRagTokens RAG 上下文最大 token 数
     * @return 裁剪后的 RAG 上下文文本
     */
    String trimRagContext(String ragContext, int maxRagTokens);

    /**
     * 估算文本的 token 数（字符数 / 2，中文场景粗略估算）。
     *
     * @param text 待估算文本
     * @return 估算的 token 数
     */
    int estimateTokens(String text);

    /**
     * 估算图片附件的 token 数（按固定值估算）。
     *
     * @param image 图片附件信息
     * @return 估算的 token 数
     */
    int estimateImageTokens(FileInfo image);
}
