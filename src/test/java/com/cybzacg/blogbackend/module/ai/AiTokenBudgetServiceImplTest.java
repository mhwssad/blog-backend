package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.ai.service.AiTokenBudgetService;
import com.cybzacg.blogbackend.module.ai.service.impl.AiTokenBudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiTokenBudgetServiceImplTest {
    private AiTokenBudgetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiTokenBudgetServiceImpl();
    }

    @Test
    void estimateTokensShouldReturnLengthDiv2() {
        assertEquals(5, service.estimateTokens("abcdefghij"));
        assertEquals(0, service.estimateTokens(null));
        assertEquals(0, service.estimateTokens(""));
    }

    @Test
    void trimHistoryShouldRemoveOldestFirst() {
        AiChatMessage m1 = msg("a".repeat(100));
        AiChatMessage m2 = msg("b".repeat(100));
        AiChatMessage m3 = msg("c".repeat(100));

        List<AiChatMessage> result = service.trimHistory(List.of(m1, m2, m3), 110);

        assertEquals(2, result.size());
        assertEquals(m2, result.get(0));
        assertEquals(m3, result.get(1));
    }

    @Test
    void trimHistoryShouldReturnAllWhenWithinBudget() {
        AiChatMessage m1 = msg("short");
        AiChatMessage m2 = msg("text");

        List<AiChatMessage> result = service.trimHistory(List.of(m1, m2), 1000);

        assertEquals(2, result.size());
    }

    @Test
    void trimRagContextShouldTruncateWhenExceeds() {
        String longContext = "a".repeat(1000);
        String result = service.trimRagContext(longContext, 100);

        assertTrue(result.endsWith("[RAG上下文已截断]"));
        assertTrue(result.length() < longContext.length());
    }

    @Test
    void trimRagContextShouldReturnOriginalWhenWithinBudget() {
        String context = "short context";
        String result = service.trimRagContext(context, 1000);

        assertEquals(context, result);
    }

    @Test
    void trimRagContextShouldReturnNullForNull() {
        assertNull(service.trimRagContext(null, 100));
    }

    @Test
    void checkBudgetShouldReturnFalseWhenInputExceeds() {
        AiChannelConfig config = new AiChannelConfig();
        config.setMaxInputTokens(3);

        AiTokenBudgetService.BudgetCheck check = service.checkBudget(
                config, "abcdefghij", List.of(), null, null);

        assertFalse(check.withinBudget());
        assertEquals(5, check.inputTokens());
    }

    @Test
    void checkBudgetShouldReturnTrueWhenAllWithinBudget() {
        AiChannelConfig config = new AiChannelConfig();
        config.setMaxInputTokens(100);
        config.setMaxHistoryTokens(100);
        config.setMaxRagTokens(100);

        AiTokenBudgetService.BudgetCheck check = service.checkBudget(
                config, "hello", List.of(msg("world")), "rag context", null);

        assertTrue(check.withinBudget());
    }

    @Test
    void checkBudgetShouldIgnoreNullBudgetLimits() {
        AiChannelConfig config = new AiChannelConfig();
        // all budget fields are null = unlimited

        AiTokenBudgetService.BudgetCheck check = service.checkBudget(
                config, "any very long input text here", List.of(), null, null);

        assertTrue(check.withinBudget());
    }

    @Test
    void estimateImageTokensShouldReturnFixedValue() {
        FileInfo image = new FileInfo();
        assertEquals(857, service.estimateImageTokens(image));
    }

    @Test
    void checkBudgetShouldReturnFalseWhenAttachmentExceeds() {
        AiChannelConfig config = new AiChannelConfig();
        config.setMaxAttachmentTokens(500);

        FileInfo img = new FileInfo();
        img.setFileType("image");

        AiTokenBudgetService.BudgetCheck check = service.checkBudget(
                config, "text", List.of(), null, List.of(img));

        assertFalse(check.withinBudget());
        assertEquals(857, check.attachmentTokens());
    }

    private AiChatMessage msg(String content) {
        AiChatMessage m = new AiChatMessage();
        m.setContent(content);
        return m;
    }
}
