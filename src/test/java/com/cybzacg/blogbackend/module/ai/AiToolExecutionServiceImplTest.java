package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.domain.ai.AiMcpServerConfig;
import com.cybzacg.blogbackend.domain.ai.AiToolAuthorization;
import com.cybzacg.blogbackend.domain.ai.AiToolCallLog;
import com.cybzacg.blogbackend.domain.ai.AiToolDefinition;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteVO;
import com.cybzacg.blogbackend.module.ai.model.internal.AiToolExecutionContext;
import com.cybzacg.blogbackend.module.ai.repository.AiMcpServerConfigRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolAuthorizationRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolCallLogRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.service.impl.AiMcpClientFactory;
import com.cybzacg.blogbackend.module.ai.service.impl.AiToolExecutionServiceImpl;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiToolExecutionServiceImplTest {

    @Mock
    private AiToolDefinitionRepository aiToolDefinitionRepository;
    @Mock
    private AiToolAuthorizationRepository aiToolAuthorizationRepository;
    @Mock
    private AiToolCallLogRepository aiToolCallLogRepository;
    @Mock
    private AiMcpServerConfigRepository aiMcpServerConfigRepository;
    @Mock
    private AiMcpClientFactory aiMcpClientFactory;
    @Mock
    private RedisOperator redisOperator;

    private AiToolExecutionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiToolExecutionServiceImpl(
                aiToolDefinitionRepository,
                aiToolAuthorizationRepository,
                aiToolCallLogRepository,
                aiMcpServerConfigRepository,
                aiMcpClientFactory,
                redisOperator
        );
        lenient().when(redisOperator.increment(any())).thenReturn(1L);
    }

    @Test
    void validateAuthorizedShouldRejectWithoutAuthorization() {
        AiToolDefinition tool = tool("tool-1", 1L, "mcp", 1);
        when(aiToolDefinitionRepository.findByToolCode("tool-1")).thenReturn(tool);
        when(aiToolAuthorizationRepository.listEnabledByToolId(1L)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.validateAuthorized("tool-1", context()));

        assertEquals(ResultErrorCode.AI_TOOL_UNAUTHORIZED.getCode(), exception.getCode());
    }

    @Test
    void executeMcpToolShouldWriteLogAndReturnSuccess() throws Exception {
        AiToolDefinition tool = tool("tool-1", 1L, "mcp", 1);
        tool.setMcpServerId(11L);
        tool.setMcpToolName("remote_tool");
        tool.setParametersSchema("{\"type\":\"object\",\"required\":[\"name\"]}");
        when(aiToolDefinitionRepository.findByToolCode("tool-1")).thenReturn(tool);

        AiToolAuthorization auth = new AiToolAuthorization();
        auth.setToolId(1L);
        auth.setAuthorizationType("agent");
        auth.setAuthorizationKey("99");
        auth.setEnabled(1);
        when(aiToolAuthorizationRepository.listEnabledByToolId(1L)).thenReturn(List.of(auth));

        AiMcpServerConfig server = new AiMcpServerConfig();
        server.setId(11L);
        server.setTransportType("http");
        server.setConnectionConfigJson("{\"url\":\"https://example.com/mcp\"}");
        server.setEnabled(1);
        server.setTimeoutSeconds(10);
        when(aiMcpServerConfigRepository.getById(11L)).thenReturn(server);

        McpClient client = mock(McpClient.class);
        when(aiMcpClientFactory.createClient(server)).thenReturn(client);
        when(client.executeTool(any(ToolExecutionRequest.class))).thenReturn(
                ToolExecutionResult.builder().resultText("{\"ok\":true}").build());

        AiToolExecuteVO result = service.execute("tool-1", "{\"name\":\"codex\"}", context());

        assertTrue(result.getSuccess());
        assertEquals("{\"ok\":true}", result.getResultText());
    }

    @Test
    void executeShouldRejectMissingRequiredArgument() {
        AiToolDefinition tool = tool("tool-1", 1L, "mcp", 1);
        tool.setParametersSchema("{\"type\":\"object\",\"required\":[\"name\"]}");
        when(aiToolDefinitionRepository.findByToolCode("tool-1")).thenReturn(tool);
        when(aiToolAuthorizationRepository.listEnabledByToolId(1L)).thenReturn(List.of(auth()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.execute("tool-1", "{\"age\":1}", context()));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
    }

    @Test
    void executeBuiltinToolShouldFailWithoutExecutor() {
        AiToolDefinition tool = tool("tool-1", 1L, "builtin", 1);
        when(aiToolDefinitionRepository.findByToolCode("tool-1")).thenReturn(tool);
        when(aiToolAuthorizationRepository.listEnabledByToolId(1L)).thenReturn(List.of(auth()));

        AiToolExecuteVO result = service.execute("tool-1", "{}", context());
        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
    }

    private AiToolDefinition tool(String code, Long id, String sourceType, Integer enabled) {
        AiToolDefinition tool = new AiToolDefinition();
        tool.setId(id);
        tool.setToolCode(code);
        tool.setToolName(code);
        tool.setSourceType(sourceType);
        tool.setEnabled(enabled);
        return tool;
    }

    private AiToolAuthorization auth() {
        AiToolAuthorization auth = new AiToolAuthorization();
        auth.setToolId(1L);
        auth.setAuthorizationType("agent");
        auth.setAuthorizationKey("99");
        auth.setEnabled(1);
        return auth;
    }

    private AiToolExecutionContext context() {
        return AiToolExecutionContext.builder()
                .userId(99L)
                .agentId(99L)
                .sceneType("agent")
                .dataScope("public_articles")
                .authorities(Set.of("ai:tool:execute"))
                .build();
    }
}
