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

    /**
     * 会话实体转用户侧 VO。
     *
     * @param session 会话实体
     * @return 会话 VO
     */
    AiSessionVO toSessionVO(AiChatSession session);

    /**
     * 会话实体转用户侧详情 VO（含渠道与模型信息）。
     *
     * @param session 会话实体
     * @return 会话详情 VO
     */
    AiSessionDetailVO toSessionDetailVO(AiChatSession session);

    /**
     * 消息实体转用户侧 VO。
     *
     * @param message 消息实体
     * @return 消息 VO
     */
    AiMessageVO toMessageVO(AiChatMessage message);

    /**
     * 渠道配置实体转后台管理 VO。
     *
     * @param config 渠道配置实体
     * @return 渠道配置 VO
     */
    AiChannelConfigVO toChannelConfigVO(AiChannelConfig config);

    /**
     * 渠道配置保存请求转实体。
     *
     * @param request 保存请求
     * @return 渠道配置实体
     */
    AiChannelConfig toChannelConfig(AiChannelConfigSaveRequest request);

    /**
     * 将保存请求中的非空字段更新到已有渠道配置实体。
     *
     * @param request 保存请求
     * @param config  待更新的渠道配置实体
     */
    @InheritConfiguration
    void updateChannelConfig(AiChannelConfigSaveRequest request, @MappingTarget AiChannelConfig config);

    /**
     * 使用日志实体转后台管理 VO。
     *
     * @param log 使用日志实体
     * @return 使用日志 VO
     */
    AiUsageLogVO toUsageLogVO(AiUsageLog log);

    /**
     * 知识源配置实体转后台管理 VO。
     *
     * @param config 知识源配置实体
     * @return 知识源配置 VO
     */
    AiKnowledgeSourceConfigVO toKnowledgeSourceConfigVO(AiKnowledgeSourceConfig config);

    /**
     * 将保存请求中的非空字段更新到已有知识源配置实体。
     *
     * @param request 保存请求
     * @param config  待更新的知识源配置实体
     */
    @InheritConfiguration
    void updateKnowledgeSourceConfig(AiKnowledgeSourceConfigSaveRequest request,
                                     @MappingTarget AiKnowledgeSourceConfig config);

    /**
     * 知识条目实体转后台管理 VO。
     *
     * @param entry 知识条目实体
     * @return 知识条目 VO
     */
    AiKnowledgeEntryVO toKnowledgeEntryVO(AiKnowledgeEntry entry);

    /**
     * 知识同步任务实体转后台管理 VO。
     *
     * @param task 同步任务实体
     * @return 同步任务 VO
     */
    AiKnowledgeSyncTaskVO toKnowledgeSyncTaskVO(AiKnowledgeSyncTask task);

    /**
     * Agent 定义实体转后台管理 VO。
     *
     * @param definition Agent 定义实体
     * @return Agent 定义 VO
     */
    AiAgentDefinitionVO toAgentDefinitionVO(AiAgentDefinition definition);

    /**
     * Agent 定义保存请求转实体。
     *
     * @param request 保存请求
     * @return Agent 定义实体
     */
    AiAgentDefinition toAgentDefinition(AiAgentDefinitionSaveRequest request);

    /**
     * 将保存请求中的非空字段更新到已有 Agent 定义实体。
     *
     * @param request   保存请求
     * @param definition 待更新的 Agent 定义实体
     */
    @InheritConfiguration
    void updateAgentDefinition(AiAgentDefinitionSaveRequest request, @MappingTarget AiAgentDefinition definition);

    /**
     * Agent 任务实体转后台管理 VO。
     *
     * @param task Agent 任务实体
     * @return Agent 任务后台 VO
     */
    AiAgentTaskAdminVO toAgentTaskAdminVO(AiAgentTask task);

    /**
     * Agent 任务实体转用户侧 VO。
     *
     * @param task Agent 任务实体
     * @return Agent 任务用户侧 VO
     */
    AiAgentTaskVO toAgentTaskVO(AiAgentTask task);

    /**
     * 消息附件实体转 VO，fileUrl 需要在外部单独填充。
     *
     * @param attachment 附件实体
     * @return 附件 VO（fileUrl 为 null）
     */
    @Mapping(target = "fileUrl", ignore = true)
    AttachmentVO toAttachmentVO(AiMessageAttachment attachment);

    /**
     * 渠道账号实体转后台管理 VO。
     *
     * @param account 渠道账号实体
     * @return 渠道账号 VO
     */
    AiChannelAccountVO toChannelAccountVO(AiChannelAccount account);

    /**
     * 渠道账号保存请求转实体。
     *
     * @param request 保存请求
     * @return 渠道账号实体
     */
    AiChannelAccount toChannelAccount(AiChannelAccountSaveRequest request);

    /**
     * 将保存请求中的非空字段更新到已有渠道账号实体。
     *
     * @param request 保存请求
     * @param account  待更新的渠道账号实体
     */
    @InheritConfiguration
    void updateChannelAccount(AiChannelAccountSaveRequest request, @MappingTarget AiChannelAccount account);

    /**
     * 映射后回调：从消息实体的 ragReferenceJson 解析引用来源并填充到 VO。
     *
     * @param message 消息实体
     * @param vo      待填充的消息 VO
     */
    @AfterMapping
    default void fillMessageRagReferences(AiChatMessage message, @MappingTarget AiMessageVO vo) {
        vo.setRagReferences(parseRagReferences(message.getRagReferenceJson()));
    }

    /**
     * 映射后回调：从使用日志实体的 ragReferenceJson 解析引用来源并填充到 VO。
     *
     * @param log 使用日志实体
     * @param vo  待填充的使用日志 VO
     */
    @AfterMapping
    default void fillUsageRagReferences(AiUsageLog log, @MappingTarget AiUsageLogVO vo) {
        vo.setRagReferences(parseRagReferences(log.getRagReferenceJson()));
    }

    /**
     * 将 JSON 字符串解析为 RAG 引用来源列表。
     *
     * @param json JSON 字符串，可为 null 或空白
     * @return 引用来源列表，解析失败时返回空列表
     */
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
