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

    AiToolDefinitionVO toToolDefinitionVO(AiToolDefinition entity);

    AiToolDefinition toToolDefinition(AiToolDefinitionSaveRequest request);

    @InheritConfiguration
    void updateToolDefinition(AiToolDefinitionSaveRequest request, @MappingTarget AiToolDefinition entity);

    AiToolAuthorizationVO toToolAuthorizationVO(AiToolAuthorization entity);

    AiToolAuthorization toToolAuthorization(AiToolAuthorizationSaveRequest request);

    @InheritConfiguration
    void updateToolAuthorization(AiToolAuthorizationSaveRequest request, @MappingTarget AiToolAuthorization entity);

    AiToolCallLogVO toToolCallLogVO(AiToolCallLog entity);

    AiMcpServerConfigVO toMcpServerConfigVO(AiMcpServerConfig entity);

    AiMcpServerConfig toMcpServerConfig(AiMcpServerConfigSaveRequest request);

    @InheritConfiguration
    void updateMcpServerConfig(AiMcpServerConfigSaveRequest request, @MappingTarget AiMcpServerConfig entity);

    AiMcpToolSnapshotVO toMcpToolSnapshotVO(AiMcpToolSnapshot entity);
}
