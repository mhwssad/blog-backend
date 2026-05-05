package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.config.property.AiRagProperties;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagHit;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagRetrievalResult;
import com.cybzacg.blogbackend.module.ai.service.AiEmbeddingService;
import com.cybzacg.blogbackend.module.ai.service.AiVectorStore;
import com.cybzacg.blogbackend.module.ai.service.impl.AiRagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRagServiceImplTest {

    @Mock
    private AiEmbeddingService aiEmbeddingService;
    @Mock
    private AiVectorStore aiVectorStore;

    private AiRagServiceImpl service;

    @BeforeEach
    void setUp() {
        AiRagProperties properties = new AiRagProperties();
        properties.setEnabled(true);
        properties.setTopK(2);
        properties.setMinScore(0.3D);
        service = new AiRagServiceImpl(properties, aiEmbeddingService, aiVectorStore);
    }

    @Test
    void retrieveShouldReturnReferencesWhenHit() {
        when(aiEmbeddingService.embed("Java RAG")).thenReturn(List.of(1F, 0F));
        when(aiVectorStore.search(anyList(), eq(2), eq(0.3D))).thenReturn(List.of(hit()));

        AiRagRetrievalResult result = service.retrieve(channel("[\"public_articles\"]"), "Java RAG");

        assertTrue(result.isEnabled());
        assertEquals(1, result.hitCount());
        assertEquals(1, result.getReferences().size());
        assertTrue(result.getContextText().contains("RAG 标题"));
        assertNotNull(result.getReferenceJson());
    }

    @Test
    void retrieveShouldSkipWhenChannelScopeDoesNotAllowPublicKnowledge() {
        AiRagRetrievalResult result = service.retrieve(channel("[\"none\"]"), "Java RAG");

        assertFalse(result.isEnabled());
        assertEquals(0, result.hitCount());
    }

    @Test
    void enrichSystemPromptShouldAppendContext() {
        AiRagRetrievalResult result = new AiRagRetrievalResult();
        result.setEnabled(true);
        result.setContextText("[引用1] 内容");

        String prompt = service.enrichSystemPrompt("base", result);

        assertTrue(prompt.contains("base"));
        assertTrue(prompt.contains("知识库片段"));
    }

    private AiChannelConfig channel(String dataScopeJson) {
        AiChannelConfig config = new AiChannelConfig();
        config.setDataScopeJson(dataScopeJson);
        return config;
    }

    private AiRagHit hit() {
        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setEntryId(1L);
        chunk.setSourceType("public_article");
        chunk.setSourceId(10L);
        chunk.setTitle("RAG 标题");
        chunk.setSourceUrl("/articles/10");
        chunk.setChunkIndex(0);
        chunk.setChunkText("RAG 内容");
        return AiRagHit.builder().chunk(chunk).score(0.9D).build();
    }
}
