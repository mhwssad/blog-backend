package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.config.property.AiRagProperties;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeChunk;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.module.ai.service.AiEmbeddingService;
import com.cybzacg.blogbackend.module.ai.service.AiVectorStore;
import com.cybzacg.blogbackend.module.ai.service.impl.AiKnowledgeChunkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeChunkServiceImplTest {

    @Mock
    private AiEmbeddingService aiEmbeddingService;
    @Mock
    private AiVectorStore aiVectorStore;

    private AiKnowledgeChunkServiceImpl service;

    @BeforeEach
    void setUp() {
        AiRagProperties properties = new AiRagProperties();
        properties.setChunkSize(100);
        properties.setChunkOverlap(20);
        service = new AiKnowledgeChunkServiceImpl(aiEmbeddingService, aiVectorStore, properties);
    }

    @Test
    void rebuildChunksShouldCreateSingleChunkForShortText() {
        when(aiEmbeddingService.embed(anyString())).thenReturn(List.of(0.1F, 0.2F));
        when(aiEmbeddingService.modelName()).thenReturn("test-embedding");

        int count = service.rebuildChunks(entry("短文本"));

        assertEquals(1, count);
        ArgumentCaptor<List<AiKnowledgeChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiVectorStore).upsertChunks(eq(1L), captor.capture());
        assertEquals("短文本", captor.getValue().get(0).getChunkText());
        assertEquals(2, captor.getValue().get(0).getEmbeddingDim());
    }

    @Test
    void rebuildChunksShouldDeleteWhenContentBlank() {
        int count = service.rebuildChunks(entry(" "));

        assertEquals(0, count);
        verify(aiVectorStore).deleteByEntryId(1L);
        verify(aiVectorStore, never()).upsertChunks(any(), any());
    }

    private AiKnowledgeEntry entry(String content) {
        AiKnowledgeEntry entry = new AiKnowledgeEntry();
        entry.setId(1L);
        entry.setSourceType("public_article");
        entry.setSourceId(10L);
        entry.setTitle("标题");
        entry.setSourceUrl("/articles/10");
        entry.setVersion(1);
        entry.setContentSnapshot(content);
        return entry;
    }
}
