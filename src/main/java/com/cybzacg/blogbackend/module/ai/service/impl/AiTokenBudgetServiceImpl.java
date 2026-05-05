package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.ai.service.AiTokenBudgetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AiTokenBudgetServiceImpl implements AiTokenBudgetService {

    private static final int IMAGE_TOKEN_ESTIMATE = 857;

    @Override
    public BudgetCheck checkBudget(AiChannelConfig config,
                                   String userInput,
                                   List<AiChatMessage> historyMessages,
                                   String ragContext,
                                   List<FileInfo> attachments) {
        int inputTokens = estimateTokens(userInput);
        int historyTokens = historyMessages.stream()
                .mapToInt(m -> estimateTokens(m.getContent() != null ? m.getContent() : ""))
                .sum();
        int ragTokens = ragContext != null ? estimateTokens(ragContext) : 0;
        int attachmentTokens = attachments != null
                ? attachments.stream().mapToInt(this::estimateImageTokens).sum() : 0;

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

        return new BudgetCheck(withinBudget, inputTokens, historyTokens, ragTokens, attachmentTokens);
    }

    @Override
    public List<AiChatMessage> trimHistory(List<AiChatMessage> messages, int maxHistoryTokens) {
        int total = messages.stream()
                .mapToInt(m -> estimateTokens(m.getContent() != null ? m.getContent() : ""))
                .sum();
        if (total <= maxHistoryTokens) {
            return messages;
        }

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

    @Override
    public int estimateTokens(String text) {
        return text == null ? 0 : text.length() / 2;
    }

    @Override
    public int estimateImageTokens(FileInfo image) {
        return IMAGE_TOKEN_ESTIMATE;
    }
}
