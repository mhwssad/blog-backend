package com.cybzacg.blogbackend.module.ai;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cybzacg.blogbackend.domain.ai.AiMcpServerConfig;
import com.cybzacg.blogbackend.domain.ai.AiMcpToolSnapshot;
import com.cybzacg.blogbackend.domain.ai.AiToolDefinition;
import com.cybzacg.blogbackend.module.ai.convert.AiToolModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpDiscoverResultVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpHealthVO;
import com.cybzacg.blogbackend.module.ai.repository.AiMcpServerConfigRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiMcpToolSnapshotRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.service.impl.AiMcpClientFactory;
import com.cybzacg.blogbackend.module.ai.service.impl.AiMcpServerAdminServiceImpl;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiMcpServerAdminServiceImplTest {

    @Mock
    private AiMcpServerConfigRepository aiMcpServerConfigRepository;
    @Mock
    private AiMcpToolSnapshotRepository aiMcpToolSnapshotRepository;
    @Mock
    private AiToolDefinitionRepository aiToolDefinitionRepository;
    @Mock
    private AiToolModelConvert aiToolModelConvert;
    @Mock
    private AiMcpClientFactory aiMcpClientFactory;
    @Mock
    private SysAuditLogService sysAuditLogService;

    private AiMcpServerAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiMcpServerAdminServiceImpl(
                aiMcpServerConfigRepository,
                aiMcpToolSnapshotRepository,
                aiToolDefinitionRepository,
                aiToolModelConvert,
                aiMcpClientFactory,
                sysAuditLogService
        );
    }

    @Test
    void healthShouldNotConnectWhenServerDisabled() {
        AiMcpServerConfig server = server(1L, 0);
        when(aiMcpServerConfigRepository.getById(1L)).thenReturn(server);

        AiMcpHealthVO result = service.checkHealth(1L);

        assertFalse(result.getHealthy());
        assertEquals("disabled", result.getStatus());
        verify(aiMcpClientFactory, never()).createClient(any());
    }

    @Test
    void discoverShouldSyncSnapshotAndToolDefinition() throws Exception {
        AiMcpServerConfig server = server(1L, 1);
        when(aiMcpServerConfigRepository.getById(1L)).thenReturn(server);
        when(aiMcpToolSnapshotRepository.getOne(any(Wrapper.class), any(Boolean.class))).thenReturn(null);
        when(aiToolDefinitionRepository.findByToolCode("mcp.1.search")).thenReturn(null);

        McpClient client = org.mockito.Mockito.mock(McpClient.class);
        ToolSpecification tool = ToolSpecification.builder()
                .name("search")
                .description("Search remote docs")
                .parameters(JsonObjectSchema.builder().addStringProperty("query").required("query").build())
                .build();
        when(aiMcpClientFactory.createClient(server)).thenReturn(client);
        when(client.listTools()).thenReturn(List.of(tool));

        AiMcpDiscoverResultVO result = service.discoverTools(1L, 99L);

        assertEquals(1, result.getDiscoveredCount());
        assertEquals(1, result.getSyncedCount());

        ArgumentCaptor<AiMcpToolSnapshot> snapshotCaptor = ArgumentCaptor.forClass(AiMcpToolSnapshot.class);
        verify(aiMcpToolSnapshotRepository).save(snapshotCaptor.capture());
        assertEquals("mcp.1.search", snapshotCaptor.getValue().getToolCode());

        ArgumentCaptor<AiToolDefinition> toolCaptor = ArgumentCaptor.forClass(AiToolDefinition.class);
        verify(aiToolDefinitionRepository).save(toolCaptor.capture());
        assertEquals("mcp", toolCaptor.getValue().getSourceType());
        assertEquals("search", toolCaptor.getValue().getMcpToolName());
    }

    private AiMcpServerConfig server(Long id, Integer enabled) {
        AiMcpServerConfig server = new AiMcpServerConfig();
        server.setId(id);
        server.setServerName("local-mcp");
        server.setTransportType("http");
        server.setConnectionConfigJson("{\"url\":\"https://example.com/mcp\"}");
        server.setEnabled(enabled);
        server.setTimeoutSeconds(10);
        return server;
    }
}
