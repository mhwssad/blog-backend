package com.cybzacg.blogbackend.module.ai.convert;

import com.cybzacg.blogbackend.domain.ai.AiMessageAttachment;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.domain.ai.AiAgentTask;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeSourceConfig;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeSyncTask;
import com.cybzacg.blogbackend.domain.ai.AiUsageLog;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeEntryVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogVO;
import com.cybzacg.blogbackend.module.ai.model.common.AiRagReferenceVO;
import com.cybzacg.blogbackend.module.ai.model.user.AttachmentVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionDetailVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionVO;
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
