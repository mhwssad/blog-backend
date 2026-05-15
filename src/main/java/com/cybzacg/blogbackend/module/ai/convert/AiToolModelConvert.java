package com.cybzacg.blogbackend.module.ai.convert;

import com.cybzacg.blogbackend.dto.domain.ai.*;
import com.cybzacg.blogbackend.module.ai.model.admin.*;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * AI 工具与 MCP 模型转换。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiToolModelConvert {

    /**
     * 工具定义实体转后台管理 VO。
     *
     * @param entity 工具定义实体
     * @return 工具定义 VO
     */
    AiToolDefinitionVO toToolDefinitionVO(AiToolDefinition entity);

    /**
     * 工具定义保存请求转实体。
     *
     * @param request 保存请求
     * @return 工具定义实体
     */
    AiToolDefinition toToolDefinition(AiToolDefinitionSaveRequest request);

    /**
     * 将保存请求中的非空字段更新到已有工具定义实体。
     *
     * @param request 保存请求
     * @param entity  待更新的工具定义实体
     */
    @InheritConfiguration
    void updateToolDefinition(AiToolDefinitionSaveRequest request, @MappingTarget AiToolDefinition entity);

    /**
     * 工具授权实体转后台管理 VO。
     *
     * @param entity 工具授权实体
     * @return 工具授权 VO
     */
    AiToolAuthorizationVO toToolAuthorizationVO(AiToolAuthorization entity);

    /**
     * 工具授权保存请求转实体。
     *
     * @param request 保存请求
     * @return 工具授权实体
     */
    AiToolAuthorization toToolAuthorization(AiToolAuthorizationSaveRequest request);

    /**
     * 将保存请求中的非空字段更新到已有工具授权实体。
     *
     * @param request 保存请求
     * @param entity  待更新的工具授权实体
     */
    @InheritConfiguration
    void updateToolAuthorization(AiToolAuthorizationSaveRequest request, @MappingTarget AiToolAuthorization entity);

    /**
     * 工具调用日志实体转后台管理 VO。
     *
     * @param entity 调用日志实体
     * @return 调用日志 VO
     */
    AiToolCallLogVO toToolCallLogVO(AiToolCallLog entity);

    /**
     * MCP 服务配置实体转后台管理 VO。
     *
     * @param entity MCP 服务配置实体
     * @return MCP 服务配置 VO
     */
    AiMcpServerConfigVO toMcpServerConfigVO(AiMcpServerConfig entity);

    /**
     * MCP 服务配置保存请求转实体。
     *
     * @param request 保存请求
     * @return MCP 服务配置实体
     */
    AiMcpServerConfig toMcpServerConfig(AiMcpServerConfigSaveRequest request);

    /**
     * 将保存请求中的非空字段更新到已有 MCP 服务配置实体。
     *
     * @param request 保存请求
     * @param entity  待更新的 MCP 服务配置实体
     */
    @InheritConfiguration
    void updateMcpServerConfig(AiMcpServerConfigSaveRequest request, @MappingTarget AiMcpServerConfig entity);

    /**
     * MCP 工具快照实体转后台管理 VO。
     *
     * @param entity MCP 工具快照实体
     * @return MCP 工具快照 VO
     */
    AiMcpToolSnapshotVO toMcpToolSnapshotVO(AiMcpToolSnapshot entity);
}
