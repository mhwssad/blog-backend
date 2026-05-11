package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeSourceConfig;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeEntryStatusEnum;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.enums.ai.ContentChangeAction;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.event.ContentChangeEvent;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeEntryRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeSourceConfigRepository;
import com.cybzacg.blogbackend.module.ai.service.impl.AiKnowledgeEntryAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeEntryAdminServiceImplTest {
    @Mock
    private AiKnowledgeEntryRepository aiKnowledgeEntryRepository;
    @Mock
    private AiKnowledgeSourceConfigRepository aiKnowledgeSourceConfigRepository;
    @Mock
    private AiModelConvert aiModelConvert;

    private AiKnowledgeEntryAdminServiceImpl aiKnowledgeEntryAdminService;

    @BeforeEach
    void setUp() {
        aiKnowledgeEntryAdminService = new AiKnowledgeEntryAdminServiceImpl(
                aiKnowledgeEntryRepository,
                aiKnowledgeSourceConfigRepository,
                aiModelConvert
        );
    }

    @Test
    void onContentChangeShouldCreateOutdatedEntryWhenPublishedSourceEnabled() {
        String sourceType = AiKnowledgeSourceTypeEnum.FORUM_POST.getCode();
        when(aiKnowledgeSourceConfigRepository.findBySourceType(sourceType)).thenReturn(enabledConfig(sourceType));
        when(aiKnowledgeEntryRepository.findBySource(sourceType, 20L)).thenReturn(null);

        aiKnowledgeEntryAdminService.onContentChange(event(sourceType, 20L, ContentChangeAction.PUBLISH));

        ArgumentCaptor<AiKnowledgeEntry> captor = ArgumentCaptor.forClass(AiKnowledgeEntry.class);
        verify(aiKnowledgeEntryRepository).save(captor.capture());
        AiKnowledgeEntry entry = captor.getValue();
        assertEquals(sourceType, entry.getSourceType());
        assertEquals(20L, entry.getSourceId());
        assertEquals(7L, entry.getAuthorId());
        assertEquals(AiKnowledgeEntryStatusEnum.OUTDATED.getValue(), entry.getStatus());
        assertEquals(0, entry.getVersion());
        assertEquals(0, entry.getChunkCount());
    }

    @Test
    void onContentChangeShouldMarkExistingEntryOutdatedWhenUpdated() {
        String sourceType = AiKnowledgeSourceTypeEnum.PUBLIC_ARTICLE.getCode();
        AiKnowledgeEntry entry = entry(sourceType, 20L, AiKnowledgeEntryStatusEnum.ACTIVE.getValue());
        when(aiKnowledgeSourceConfigRepository.findBySourceType(sourceType)).thenReturn(enabledConfig(sourceType));
        when(aiKnowledgeEntryRepository.findBySource(sourceType, 20L)).thenReturn(entry);

        aiKnowledgeEntryAdminService.onContentChange(event(sourceType, 20L, ContentChangeAction.UPDATE));

        assertEquals(AiKnowledgeEntryStatusEnum.OUTDATED.getValue(), entry.getStatus());
        verify(aiKnowledgeEntryRepository).updateById(entry);
    }

    @Test
    void onContentChangeShouldNotReviveDeletedEntryWhenUpdated() {
        String sourceType = AiKnowledgeSourceTypeEnum.PUBLIC_ARTICLE.getCode();
        AiKnowledgeEntry entry = entry(sourceType, 20L, AiKnowledgeEntryStatusEnum.DELETED.getValue());
        when(aiKnowledgeSourceConfigRepository.findBySourceType(sourceType)).thenReturn(enabledConfig(sourceType));
        when(aiKnowledgeEntryRepository.findBySource(sourceType, 20L)).thenReturn(entry);

        aiKnowledgeEntryAdminService.onContentChange(event(sourceType, 20L, ContentChangeAction.UPDATE));

        verify(aiKnowledgeEntryRepository, never()).updateById(any(AiKnowledgeEntry.class));
        assertEquals(AiKnowledgeEntryStatusEnum.DELETED.getValue(), entry.getStatus());
    }

    @Test
    void onContentChangeShouldDisableExistingEntryWhenHidden() {
        String sourceType = AiKnowledgeSourceTypeEnum.FORUM_POST.getCode();
        AiKnowledgeEntry entry = entry(sourceType, 20L, AiKnowledgeEntryStatusEnum.ACTIVE.getValue());
        when(aiKnowledgeSourceConfigRepository.findBySourceType(sourceType)).thenReturn(enabledConfig(sourceType));
        when(aiKnowledgeEntryRepository.findBySource(sourceType, 20L)).thenReturn(entry);

        aiKnowledgeEntryAdminService.onContentChange(event(sourceType, 20L, ContentChangeAction.HIDE));

        assertEquals(AiKnowledgeEntryStatusEnum.DISABLED.getValue(), entry.getStatus());
        verify(aiKnowledgeEntryRepository).updateById(entry);
    }

    @Test
    void onContentChangeShouldMarkExistingEntryDeletedWhenDeleted() {
        String sourceType = AiKnowledgeSourceTypeEnum.FORUM_POST.getCode();
        AiKnowledgeEntry entry = entry(sourceType, 20L, AiKnowledgeEntryStatusEnum.ACTIVE.getValue());
        when(aiKnowledgeSourceConfigRepository.findBySourceType(sourceType)).thenReturn(enabledConfig(sourceType));
        when(aiKnowledgeEntryRepository.findBySource(sourceType, 20L)).thenReturn(entry);

        aiKnowledgeEntryAdminService.onContentChange(event(sourceType, 20L, ContentChangeAction.DELETE));

        assertEquals(AiKnowledgeEntryStatusEnum.DELETED.getValue(), entry.getStatus());
        verify(aiKnowledgeEntryRepository).updateById(entry);
    }

    @Test
    void onContentChangeShouldIgnoreUnknownSourceType() {
        aiKnowledgeEntryAdminService.onContentChange(event("private_chat", 20L, ContentChangeAction.PUBLISH));

        verify(aiKnowledgeSourceConfigRepository, never()).findBySourceType(any());
        verify(aiKnowledgeEntryRepository, never()).save(any(AiKnowledgeEntry.class));
    }

    @Test
    void onContentChangeShouldIgnoreDisabledSourceConfig() {
        String sourceType = AiKnowledgeSourceTypeEnum.FORUM_POST.getCode();
        AiKnowledgeSourceConfig config = enabledConfig(sourceType);
        config.setEnabled(0);
        when(aiKnowledgeSourceConfigRepository.findBySourceType(sourceType)).thenReturn(config);

        aiKnowledgeEntryAdminService.onContentChange(event(sourceType, 20L, ContentChangeAction.PUBLISH));

        verify(aiKnowledgeEntryRepository, never()).findBySource(any(), any());
        verify(aiKnowledgeEntryRepository, never()).save(any(AiKnowledgeEntry.class));
    }

    private ContentChangeEvent event(String sourceType, Long sourceId, ContentChangeAction action) {
        return new ContentChangeEvent(sourceType, sourceId, action, 7L);
    }

    private AiKnowledgeSourceConfig enabledConfig(String sourceType) {
        AiKnowledgeSourceConfig config = new AiKnowledgeSourceConfig();
        config.setSourceType(sourceType);
        config.setEnabled(1);
        return config;
    }

    private AiKnowledgeEntry entry(String sourceType, Long sourceId, Integer status) {
        AiKnowledgeEntry entry = new AiKnowledgeEntry();
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        entry.setStatus(status);
        return entry;
    }
}
