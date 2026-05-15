package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.ai.service.AiTokenBudgetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Token 预算管理服务实现。
 *
 * <p>负责在模型调用前检查各类 token 预算（用户输入、历史上下文、RAG 知识、附件），
 * 并提供历史消息裁剪和 RAG 上下文截断能力，确保单次调用不超过渠道配置的上限。
 *
 * <p>token 估算策略：中英文混合场景下采用 {@code 字符数 / 2} 进行粗略估算，
 * 图片附件按固定值 {@link #IMAGE_TOKEN_ESTIMATE} 计。
 */
@Slf4j
@Service
public class AiTokenBudgetServiceImpl implements AiTokenBudgetService {

    /** 单张图片附件的固定 token 估算值 */
    private static final int IMAGE_TOKEN_ESTIMATE = 857;

    /**
     * 检查当前请求的各类 token 用量是否均在渠道配置预算内。
     *
     * @param config          渠道配置，包含各类 token 上限
     * @param userInput       用户输入文本
     * @param historyMessages 历史对话消息列表
     * @param ragContext       RAG 检索到的知识上下文文本，可为 null
     * @param attachments     附件（图片）列表，可为 null
     * @return 预算检查结果，包含是否在预算内及各类 token 估算值
     */
    @Override
    public BudgetCheck checkBudget(AiChannelConfig config,
                                   String userInput,
                                   List<AiChatMessage> historyMessages,
                                   String ragContext,
                                   List<FileInfo> attachments) {
        // 分别估算各类 token 用量
        int inputTokens = estimateTokens(userInput);
        int historyTokens = historyMessages.stream()
                .mapToInt(m -> estimateTokens(m.getContent() != null ? m.getContent() : ""))
                .sum();
        int ragTokens = ragContext != null ? estimateTokens(ragContext) : 0;
        int attachmentTokens = attachments != null
                ? attachments.stream().mapToInt(this::estimateImageTokens).sum() : 0;

        // 逐项对比渠道配置上限，仅当配置了有效上限（> 0）时才检查
        boolean withinBudget = true;
        if (config.getMaxInputTokens() != null && config.getMaxInputTokens() > 0 && inputTokens > config.getMaxInputTokens()) {
            withinBudget = false;
        }
        if (config.getMaxHistoryTokens() != null && config.getMaxHistoryTokens() > 0 && historyTokens > config.getMaxHistoryTokens()) {
            withinBudget = false;
        }
        if (config.getMaxRagTokens() != null && config.getMaxRagTokens() > 0 && ragTokens > config.getMaxRagTokens()) {
            withinBudget = false;
        }
        if (config.getMaxAttachmentTokens() != null && config.getMaxAttachmentTokens() > 0 && attachmentTokens > config.getMaxAttachmentTokens()) {
            withinBudget = false;
        }

        if (!withinBudget) {
            log.debug("Token 预算超限: input={}, history={}, rag={}, attachment={}",
                    inputTokens, historyTokens, ragTokens, attachmentTokens);
        }

        return new BudgetCheck(withinBudget, inputTokens, historyTokens, ragTokens, attachmentTokens);
    }

    /**
     * 从最老的消息开始裁剪历史对话，直到总 token 数不超过上限。
     *
     * @param messages         原始历史消息列表
     * @param maxHistoryTokens 历史消息允许的最大 token 数
     * @return 裁剪后的消息列表，保证总估算 token 数 ≤ maxHistoryTokens
     */
    @Override
    public List<AiChatMessage> trimHistory(List<AiChatMessage> messages, int maxHistoryTokens) {
        int total = messages.stream()
                .mapToInt(m -> estimateTokens(m.getContent() != null ? m.getContent() : ""))
                .sum();
        if (total <= maxHistoryTokens) {
            return messages;
        }

        // 逐条移除最早的消息，直到剩余消息总 token 数满足上限
        List<AiChatMessage> trimmed = new ArrayList<>(messages);
        while (!trimmed.isEmpty()) {
            int current = trimmed.stream()
                    .mapToInt(m -> estimateTokens(m.getContent() != null ? m.getContent() : ""))
                    .sum();
            if (current <= maxHistoryTokens) {
                break;
            }
            trimmed.remove(0);
        }
        return trimmed;
    }

    /**
     * 按 token 上限截断 RAG 上下文文本。
     *
     * <p>按 {@code token数 × 2} 换算最大字符数，超出部分截断并追加提示后缀。
     *
     * @param ragContext    原始 RAG 上下文文本，为 null 时直接返回 null
     * @param maxRagTokens  RAG 上下文允许的最大 token 数
     * @return 截断后的文本，可能带有截断提示后缀
     */
    @Override
    public String trimRagContext(String ragContext, int maxRagTokens) {
        if (ragContext == null) {
            return null;
        }
        int maxChars = maxRagTokens * 2;
        return ragContext.length() > maxChars
                ? ragContext.substring(0, maxChars) + "...[RAG上下文已截断]"
                : ragContext;
    }

    /**
     * 粗略估算文本的 token 数（字符数 / 2）。
     *
     * @param text 待估算文本，为 null 时返回 0
     * @return 估算的 token 数
     */
    @Override
    public int estimateTokens(String text) {
        return text == null ? 0 : text.length() / 2;
    }

    /**
     * 估算单张图片附件的 token 数，返回固定值。
     *
     * @param image 图片附件信息
     * @return 固定估算值 {@link #IMAGE_TOKEN_ESTIMATE}
     */
    @Override
    public int estimateImageTokens(FileInfo image) {
        return IMAGE_TOKEN_ESTIMATE;
    }
}
