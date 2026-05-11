package com.cybzacg.blogbackend.module.ai.convert;

import com.cybzacg.blogbackend.dto.domain.ai.*;
import com.cybzacg.blogbackend.module.ai.model.admin.*;
import com.cybzacg.blogbackend.module.ai.model.common.AiRagReferenceVO;
import com.cybzacg.blogbackend.module.ai.model.user.*;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.mapstruct.*;

import java.util.List;

/**
 * AI模块对象转换。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiModelConvert {

    AiSessionVO toSessionVO(AiChatSession session);

    AiSessionDetailVO toSessionDetailVO(AiChatSession session);

    AiMessageVO toMessageVO(AiChatMessage message);

    AiChannelConfigVO toChannelConfigVO(AiChannelConfig config);

    AiChannelConfig toChannelConfig(AiChannelConfigSaveRequest request);

    @InheritConfiguration
    void updateChannelConfig(AiChannelConfigSaveRequest request, @MappingTarget AiChannelConfig config);

    AiUsageLogVO toUsageLogVO(AiUsageLog log);

    AiKnowledgeSourceConfigVO toKnowledgeSourceConfigVO(AiKnowledgeSourceConfig config);

    @InheritConfiguration
    void updateKnowledgeSourceConfig(AiKnowledgeSourceConfigSaveRequest request,
                                     @MappingTarget AiKnowledgeSourceConfig config);

    AiKnowledgeEntryVO toKnowledgeEntryVO(AiKnowledgeEntry entry);

    AiKnowledgeSyncTaskVO toKnowledgeSyncTaskVO(AiKnowledgeSyncTask task);

    AiAgentDefinitionVO toAgentDefinitionVO(AiAgentDefinition definition);

    AiAgentDefinition toAgentDefinition(AiAgentDefinitionSaveRequest request);

    @InheritConfiguration
    void updateAgentDefinition(AiAgentDefinitionSaveRequest request, @MappingTarget AiAgentDefinition definition);

    AiAgentTaskAdminVO toAgentTaskAdminVO(AiAgentTask task);

    AiAgentTaskVO toAgentTaskVO(AiAgentTask task);

    @Mapping(target = "fileUrl", ignore = true)
    AttachmentVO toAttachmentVO(AiMessageAttachment attachment);

    AiChannelAccountVO toChannelAccountVO(AiChannelAccount account);

    AiChannelAccount toChannelAccount(AiChannelAccountSaveRequest request);

    @InheritConfiguration
    void updateChannelAccount(AiChannelAccountSaveRequest request, @MappingTarget AiChannelAccount account);

    @AfterMapping
    default void fillMessageRagReferences(AiChatMessage message, @MappingTarget AiMessageVO vo) {
        vo.setRagReferences(parseRagReferences(message.getRagReferenceJson()));
    }

    @AfterMapping
    default void fillUsageRagReferences(AiUsageLog log, @MappingTarget AiUsageLogVO vo) {
        vo.setRagReferences(parseRagReferences(log.getRagReferenceJson()));
    }

    default List<AiRagReferenceVO> parseRagReferences(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<AiRagReferenceVO> references = JsonUtils.fromJson(json, new TypeReference<>() {
            });
            return references == null ? List.of() : references;
        } catch (RuntimeException ex) {
            return List.of();
        }
    }
}
