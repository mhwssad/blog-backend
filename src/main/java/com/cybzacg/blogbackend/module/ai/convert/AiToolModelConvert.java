package com.cybzacg.blogbackend.module.ai.convert;

import com.cybzacg.blogbackend.domain.ai.AiMcpServerConfig;
import com.cybzacg.blogbackend.domain.ai.AiMcpToolSnapshot;
import com.cybzacg.blogbackend.domain.ai.AiToolAuthorization;
import com.cybzacg.blogbackend.domain.ai.AiToolCallLog;
import com.cybzacg.blogbackend.domain.ai.AiToolDefinition;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpServerConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpServerConfigVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpToolSnapshotVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolAuthorizationSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolAuthorizationVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolCallLogVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolDefinitionVO;
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
