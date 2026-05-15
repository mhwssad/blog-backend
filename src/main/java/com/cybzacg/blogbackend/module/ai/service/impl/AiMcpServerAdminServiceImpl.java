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

    /**
     * 分页查询 MCP 服务配置，支持按名称、传输类型、启用状态过滤。
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
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

    /**
     * 根据 ID 获取 MCP 服务配置详情。
     *
     * @param id MCP 服务配置 ID
     * @return 服务配置 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 服务不存在时抛出
     */
    @Override
    public AiMcpServerConfigVO getServer(Long id) {
        return aiToolModelConvert.toMcpServerConfigVO(getServerOrThrow(id));
    }

    /**
     * 创建 MCP 服务配置，名称唯一性校验通过后持久化并记录审计日志。
     *
     * @param request    创建请求
     * @param operatorId 操作人 ID
     * @return 创建后的服务配置 VO
     */
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
        log.info("创建 MCP 服务配置: id={}, serverName={}, operatorId={}", entity.getId(), entity.getServerName(), operatorId);
        return aiToolModelConvert.toMcpServerConfigVO(entity);
    }

    /**
     * 更新 MCP 服务配置，变更名称时校验唯一性，更新前后均记录审计日志。
     *
     * @param id         服务配置 ID
     * @param request    更新请求
     * @param operatorId 操作人 ID
     * @return 更新后的服务配置 VO
     */
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

    /**
     * 切换 MCP 服务的启用/停用状态。
     *
     * @param id         服务配置 ID
     * @param enabled    目标状态（0=停用，1=启用）
     * @param operatorId 操作人 ID
     */
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

    /**
     * 软删除 MCP 服务配置：将状态设为停用。
     *
     * @param id         服务配置 ID
     * @param operatorId 操作人 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServer(Long id, Long operatorId) {
        AiMcpServerConfig entity = getServerOrThrow(id);
        entity.setEnabled(0);
        entity.setUpdatedBy(operatorId);
        aiMcpServerConfigRepository.updateById(entity);
        recordAudit(operatorId, id, "enabled=1", "enabled=0");
    }

    /**
     * 连接 MCP 服务并发现其提供的工具列表，同步到快照和工具定义表。
     * 发现成功后更新服务健康状态为 healthy，失败则标记为 unhealthy。
     *
     * @param id         MCP 服务配置 ID
     * @param operatorId 操作人 ID
     * @return 发现结果，包含发现数量和同步数量
     */
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
            log.info("MCP 工具发现成功: serverId={}, discoveredCount={}, syncedCount={}", id, tools.size(), synced);

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

    /**
     * 查询指定 MCP 服务下的工具快照列表。
     *
     * @param id MCP 服务配置 ID
     * @return 工具快照 VO 列表
     */
    @Override
    public List<AiMcpToolSnapshotVO> listTools(Long id) {
        getServerOrThrow(id);
        return aiMcpToolSnapshotRepository.listByServerId(id).stream()
                .map(aiToolModelConvert::toMcpToolSnapshotVO)
                .toList();
    }

    /**
     * 检查 MCP 服务健康状态，更新 lastHealthStatus 并返回检查结果。
     *
     * @param id MCP 服务配置 ID
     * @return 健康检查结果 VO
     */
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

    /**
     * 将单个 MCP 工具同步到快照表和工具定义表：存在则更新，不存在则插入。
     * 工具编码格式为 mcp.{serverId}.{sanitizedToolName}。
     *
     * @param server     MCP 服务配置
     * @param tool       LangChain4j 工具规格
     * @param operatorId 操作人 ID
     */
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

    /**
     * 按服务 ID 和工具名称查找快照记录。
     *
     * @param serverId MCP 服务 ID
     * @param toolName 工具名称
     * @return 快照记录，不存在时返回 null
     */
    private AiMcpToolSnapshot findSnapshot(Long serverId, String toolName) {
        return aiMcpToolSnapshotRepository.getOne(new LambdaQueryWrapper<AiMcpToolSnapshot>()
                .eq(AiMcpToolSnapshot::getMcpServerId, serverId)
                .eq(AiMcpToolSnapshot::getMcpToolName, toolName)
                .last("limit 1"), false);
    }

    /**
     * 构建工具编码，将工具名称中的非字母数字字符替换为下划线。
     * 格式：mcp.{serverId}.{sanitizedToolName}
     *
     * @param serverId MCP 服务 ID
     * @param toolName 原始工具名称
     * @return 规范化后的工具编码
     */
    private String buildToolCode(Long serverId, String toolName) {
        String sanitized = toolName == null ? "unknown" : toolName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return "mcp." + serverId + "." + sanitized;
    }

    /**
     * 按 ID 查询 MCP 服务配置，不存在时抛出业务异常。
     *
     * @param id MCP 服务配置 ID
     * @return 服务配置实体
     */
    private AiMcpServerConfig getServerOrThrow(Long id) {
        return ExceptionThrowerCore.requireNonNull(
                aiMcpServerConfigRepository.getById(id),
                ResultErrorCode.AI_MCP_SERVER_NOT_FOUND);
    }

    /**
     * 记录 MCP 服务配置变更审计日志。
     *
     * @param operatorId 操作人 ID
     * @param targetId   目标 MCP 服务配置 ID
     * @param before     变更前状态摘要
     * @param after      变更后状态摘要
     */
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
