package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.dto.domain.ai.AiMcpServerConfig;
import com.cybzacg.blogbackend.enums.ai.AiMcpTransportTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 根据数据库 MCP 配置构建 LangChain4j MCP 客户端。
 */
@Component
public class AiMcpClientFactory {

    /**
     * 创建一次性 MCP 客户端，调用方负责关闭。
     */
    public McpClient createClient(AiMcpServerConfig serverConfig) {
        Map<String, Object> connection = AiToolSupport.parseJsonObject(
                serverConfig.getConnectionConfigJson(), "MCP 连接配置无效");
        Map<String, Object> auth = AiToolSupport.parseJsonObject(
                serverConfig.getAuthConfigJson(), "MCP 鉴权配置无效");
        Duration timeout = Duration.ofSeconds(serverConfig.getTimeoutSeconds() == null
                ? 60 : serverConfig.getTimeoutSeconds());

        McpTransport transport = buildTransport(serverConfig, connection, auth, timeout);
        return DefaultMcpClient.builder()
                .key("mcp-" + serverConfig.getId())
                .clientName("blog-backend")
                .clientVersion("1.0.0")
                .transport(transport)
                .initializationTimeout(timeout)
                .toolExecutionTimeout(timeout)
                .pingTimeout(timeout)
                .cacheToolList(false)
                .build();
    }

    private McpTransport buildTransport(AiMcpServerConfig serverConfig, Map<String, Object> connection,
                                        Map<String, Object> auth, Duration timeout) {
        AiMcpTransportTypeEnum transportType = AiMcpTransportTypeEnum.fromCode(serverConfig.getTransportType());
        if (transportType == AiMcpTransportTypeEnum.STDIO) {
            return buildStdioTransport(connection);
        }
        if (transportType == AiMcpTransportTypeEnum.HTTP) {
            return buildHttpTransport(connection, auth, timeout);
        }
        return ExceptionThrowerCore.throwBusiness(ResultErrorCode.AI_MCP_TRANSPORT_INVALID);
    }

    private StdioMcpTransport buildStdioTransport(Map<String, Object> connection) {
        Object command = connection.get("command");
        ExceptionThrowerCore.throwBusinessIf(!(command instanceof List<?>),
                ResultErrorCode.ILLEGAL_ARGUMENT, "stdio MCP 连接配置必须包含 command 数组");
        List<?> commandList = (List<?>) command;
        ExceptionThrowerCore.throwBusinessIf(commandList.isEmpty(),
                ResultErrorCode.ILLEGAL_ARGUMENT, "stdio MCP 连接配置必须包含 command 数组");
        List<String> commandArgs = commandList.stream().map(String::valueOf).toList();

        StdioMcpTransport.Builder builder = StdioMcpTransport.builder().command(commandArgs);
        Object environment = connection.get("environment");
        if (environment instanceof Map<?, ?> environmentMap && !environmentMap.isEmpty()) {
            builder.environment(environmentMap.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            entry -> String.valueOf(entry.getValue()))));
        }
        return builder.build();
    }

    private StreamableHttpMcpTransport buildHttpTransport(Map<String, Object> connection,
                                                          Map<String, Object> auth,
                                                          Duration timeout) {
        String url = String.valueOf(connection.getOrDefault("url", ""));
        ExceptionThrowerCore.throwBusinessIf(!StrUtils.hasText(url),
                ResultErrorCode.ILLEGAL_ARGUMENT, "http MCP 连接配置必须包含 url");

        StreamableHttpMcpTransport.Builder builder = StreamableHttpMcpTransport.builder()
                .url(url)
                .timeout(timeout);
        Map<String, String> headers = buildHeaders(connection, auth);
        if (!headers.isEmpty()) {
            builder.customHeaders(headers);
        }
        return builder.build();
    }

    private Map<String, String> buildHeaders(Map<String, Object> connection, Map<String, Object> auth) {
        java.util.LinkedHashMap<String, String> headers = new java.util.LinkedHashMap<>();
        Object connectionHeaders = connection.get("headers");
        if (connectionHeaders instanceof Map<?, ?> headerMap) {
            headerMap.forEach((key, value) -> headers.put(String.valueOf(key), String.valueOf(value)));
        }
        Object authHeaders = auth.get("headers");
        if (authHeaders instanceof Map<?, ?> headerMap) {
            headerMap.forEach((key, value) -> headers.put(String.valueOf(key), String.valueOf(value)));
        }
        String bearerToken = String.valueOf(auth.getOrDefault("bearerToken", ""));
        if (StrUtils.hasText(bearerToken)) {
            headers.put("Authorization", "Bearer " + bearerToken);
        }
        return headers;
    }
}
