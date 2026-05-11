package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiMcpServerConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiMcpToolSnapshot;
import com.cybzacg.blogbackend.dto.domain.ai.AiToolDefinition;
import com.cybzacg.blogbackend.dto.repository.ai.AiMcpServerConfigRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiMcpToolSnapshotRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiToolDefinitionRepository;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.ai.AiToolRiskLevelEnum;
import com.cybzacg.blogbackend.enums.ai.AiToolSourceTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiToolModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.*;
import com.cybzacg.blogbackend.module.ai.service.AiMcpServerAdminService;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台 MCP 服务管理实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMcpServerAdminServiceImpl implements AiMcpServerAdminService {

    private static final String DEFAULT_USE_SCENARIOS = "[\"agent\"]";

    private final AiMcpServerConfigRepository aiMcpServerConfigRepository;
    private final AiMcpToolSnapshotRepository aiMcpToolSnapshotRepository;
    private final AiToolDefinitionRepository aiToolDefinitionRepository;
    private final AiToolModelConvert aiToolModelConvert;
    private final AiMcpClientFactory aiMcpClientFactory;
    private final SysAuditLogService sysAuditLogService;

    @Override
    public PageResult<AiMcpServerConfigVO> pageServers(AiMcpServerConfigPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        Page<AiMcpServerConfig> page = aiMcpServerConfigRepository.page(new Page<>(current, size),
                new LambdaQueryWrapper<AiMcpServerConfig>()
                        .like(StrUtils.hasText(query.getServerName()), AiMcpServerConfig::getServerName, query.getServerName())
                        .eq(StrUtils.hasText(query.getTransportType()), AiMcpServerConfig::getTransportType, query.getTransportType())
                        .eq(query.getEnabled() != null, AiMcpServerConfig::getEnabled, query.getEnabled())
                        .orderByDesc(AiMcpServerConfig::getId));
        List<AiMcpServerConfigVO> records = page.getRecords().stream()
                .map(aiToolModelConvert::toMcpServerConfigVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public AiMcpServerConfigVO getServer(Long id) {
        return aiToolModelConvert.toMcpServerConfigVO(getServerOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiMcpServerConfigVO createServer(AiMcpServerConfigSaveRequest request, Long operatorId) {
        ExceptionThrowerCore.throwBusinessIfNotNull(
                aiMcpServerConfigRepository.findByServerName(request.getServerName()),
                ResultErrorCode.DATA_ALREADY_EXISTS, "MCP 服务名称已存在");
        AiMcpServerConfig entity = aiToolModelConvert.toMcpServerConfig(request);
        entity.setLastHealthStatus("unknown");
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        aiMcpServerConfigRepository.save(entity);
        recordAudit(operatorId, entity.getId(), null, "create:" + entity.getServerName());
        return aiToolModelConvert.toMcpServerConfigVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiMcpServerConfigVO updateServer(Long id, AiMcpServerConfigSaveRequest request, Long operatorId) {
        AiMcpServerConfig entity = getServerOrThrow(id);
        AiMcpServerConfig existing = aiMcpServerConfigRepository.findByServerName(request.getServerName());
        ExceptionThrowerCore.throwBusinessIf(existing != null && !existing.getId().equals(id),
                ResultErrorCode.DATA_ALREADY_EXISTS, "MCP 服务名称已存在");
        String before = entity.getServerName() + ":" + entity.getTransportType() + ":" + entity.getEnabled();
        aiToolModelConvert.updateMcpServerConfig(request, entity);
        entity.setUpdatedBy(operatorId);
        aiMcpServerConfigRepository.updateById(entity);
        recordAudit(operatorId, entity.getId(), before,
                entity.getServerName() + ":" + entity.getTransportType() + ":" + entity.getEnabled());
        return aiToolModelConvert.toMcpServerConfigVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServerStatus(Long id, Integer enabled, Long operatorId) {
        AiToolSupport.validateEnabled(enabled);
        AiMcpServerConfig entity = getServerOrThrow(id);
        Integer before = entity.getEnabled();
        entity.setEnabled(enabled);
        entity.setUpdatedBy(operatorId);
        aiMcpServerConfigRepository.updateById(entity);
        recordAudit(operatorId, id, String.valueOf(before), String.valueOf(enabled));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServer(Long id, Long operatorId) {
        AiMcpServerConfig entity = getServerOrThrow(id);
        entity.setEnabled(0);
        entity.setUpdatedBy(operatorId);
        aiMcpServerConfigRepository.updateById(entity);
        recordAudit(operatorId, id, "enabled=1", "enabled=0");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiMcpDiscoverResultVO discoverTools(Long id, Long operatorId) {
        AiMcpServerConfig server = getServerOrThrow(id);
        ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(1).equals(server.getEnabled()),
                ResultErrorCode.AI_MCP_SERVER_DISABLED);
        try (McpClient client = aiMcpClientFactory.createClient(server)) {
            List<ToolSpecification> tools = client.listTools();
            int synced = 0;
            for (ToolSpecification tool : tools) {
                syncTool(server, tool, operatorId);
                synced++;
            }
            server.setLastHealthStatus("healthy");
            server.setLastDiscoveredAt(LocalDateTime.now());
            server.setLastErrorSummary(null);
            server.setUpdatedBy(operatorId);
            aiMcpServerConfigRepository.updateById(server);

            AiMcpDiscoverResultVO vo = new AiMcpDiscoverResultVO();
            vo.setDiscoveredCount(tools.size());
            vo.setSyncedCount(synced);
            return vo;
        } catch (Exception e) {
            log.warn("MCP 工具发现失败: serverId={}", id, e);
            server.setLastHealthStatus("unhealthy");
            server.setLastErrorSummary(AiToolSupport.summarize(e.getMessage()));
            aiMcpServerConfigRepository.updateById(server);
            return ExceptionThrowerCore.throwBusiness(ResultErrorCode.AI_MCP_DISCOVERY_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public List<AiMcpToolSnapshotVO> listTools(Long id) {
        getServerOrThrow(id);
        return aiMcpToolSnapshotRepository.listByServerId(id).stream()
                .map(aiToolModelConvert::toMcpToolSnapshotVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiMcpHealthVO checkHealth(Long id) {
        AiMcpServerConfig server = getServerOrThrow(id);
        AiMcpHealthVO vo = new AiMcpHealthVO();
        if (!Integer.valueOf(1).equals(server.getEnabled())) {
            vo.setHealthy(false);
            vo.setStatus("disabled");
            vo.setErrorSummary("MCP 服务已停用");
            return vo;
        }
        try (McpClient client = aiMcpClientFactory.createClient(server)) {
            client.checkHealth();
            server.setLastHealthStatus("healthy");
            server.setLastErrorSummary(null);
            aiMcpServerConfigRepository.updateById(server);
            vo.setHealthy(true);
            vo.setStatus("healthy");
            return vo;
        } catch (Exception e) {
            server.setLastHealthStatus("unhealthy");
            server.setLastErrorSummary(AiToolSupport.summarize(e.getMessage()));
            aiMcpServerConfigRepository.updateById(server);
            vo.setHealthy(false);
            vo.setStatus("unhealthy");
            vo.setErrorSummary(server.getLastErrorSummary());
            return vo;
        }
    }

    private void syncTool(AiMcpServerConfig server, ToolSpecification tool, Long operatorId) {
        String toolCode = buildToolCode(server.getId(), tool.name());
        String parametersSchema = tool.parameters() == null ? null : JsonUtils.toJson(tool.parameters());
        java.util.LinkedHashMap<String, Object> raw = new java.util.LinkedHashMap<>();
        raw.put("name", tool.name());
        raw.put("description", tool.description());
        raw.put("parameters", tool.parameters());
        String rawDefinition = JsonUtils.toJson(raw);

        AiMcpToolSnapshot snapshot = findSnapshot(server.getId(), tool.name());
        if (snapshot == null) {
            snapshot = new AiMcpToolSnapshot();
            snapshot.setMcpServerId(server.getId());
            snapshot.setMcpToolName(tool.name());
        }
        snapshot.setToolCode(toolCode);
        snapshot.setToolName(tool.name());
        snapshot.setDescription(tool.description());
        snapshot.setParametersSchema(parametersSchema);
        snapshot.setRiskLevel(AiToolRiskLevelEnum.MEDIUM.getCode());
        snapshot.setUseScenarios(DEFAULT_USE_SCENARIOS);
        snapshot.setEnabled(1);
        snapshot.setDiscoveredAt(LocalDateTime.now());
        snapshot.setRawDefinitionJson(rawDefinition);
        if (snapshot.getId() == null) {
            aiMcpToolSnapshotRepository.save(snapshot);
        } else {
            aiMcpToolSnapshotRepository.updateById(snapshot);
        }

        AiToolDefinition definition = aiToolDefinitionRepository.findByToolCode(toolCode);
        if (definition == null) {
            definition = new AiToolDefinition();
            definition.setToolCode(toolCode);
            definition.setCreatedBy(operatorId);
        }
        definition.setToolName(tool.name());
        definition.setSourceType(AiToolSourceTypeEnum.MCP.getCode());
        definition.setMcpServerId(server.getId());
        definition.setMcpToolName(tool.name());
        definition.setDescription(tool.description());
        definition.setParametersSchema(parametersSchema);
        definition.setRiskLevel(AiToolRiskLevelEnum.MEDIUM.getCode());
        definition.setUseScenarios(DEFAULT_USE_SCENARIOS);
        definition.setEnabled(1);
        definition.setUpdatedBy(operatorId);
        if (definition.getId() == null) {
            aiToolDefinitionRepository.save(definition);
        } else {
            aiToolDefinitionRepository.updateById(definition);
        }
    }

    private AiMcpToolSnapshot findSnapshot(Long serverId, String toolName) {
        return aiMcpToolSnapshotRepository.getOne(new LambdaQueryWrapper<AiMcpToolSnapshot>()
                .eq(AiMcpToolSnapshot::getMcpServerId, serverId)
                .eq(AiMcpToolSnapshot::getMcpToolName, toolName)
                .last("limit 1"), false);
    }

    private String buildToolCode(Long serverId, String toolName) {
        String sanitized = toolName == null ? "unknown" : toolName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return "mcp." + serverId + "." + sanitized;
    }

    private AiMcpServerConfig getServerOrThrow(Long id) {
        return ExceptionThrowerCore.requireNonNull(
                aiMcpServerConfigRepository.getById(id),
                ResultErrorCode.AI_MCP_SERVER_NOT_FOUND);
    }

    private void recordAudit(Long operatorId, Long targetId, String before, String after) {
        SysAuditLogCreateRequest audit = new SysAuditLogCreateRequest();
        audit.setOperatorUserId(operatorId);
        audit.setOperationType(SysAuditOperationType.MODIFY_AI_MCP_SERVER.getCode());
        audit.setTargetTypeName("AiMcpServerConfig");
        audit.setTargetId(targetId);
        audit.setBeforeState(before);
        audit.setAfterState(after);
        sysAuditLogService.record(audit);
    }
}
