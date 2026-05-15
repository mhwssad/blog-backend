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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 根据数据库 MCP 配置构建 LangChain4j MCP 客户端。
 */
@Slf4j
@Component
public class AiMcpClientFactory {

    /**
     * 创建一次性 MCP 客户端，调用方负责关闭。
     */
    public McpClient createClient(AiMcpServerConfig serverConfig) {
        // 解析 JSON 配置为 Map，格式错误时直接抛出业务异常
        Map<String, Object> connection = AiToolSupport.parseJsonObject(
                serverConfig.getConnectionConfigJson(), "MCP 连接配置无效");
        Map<String, Object> auth = AiToolSupport.parseJsonObject(
                serverConfig.getAuthConfigJson(), "MCP 鉴权配置无效");
        // 默认超时 60 秒
        Duration timeout = Duration.ofSeconds(serverConfig.getTimeoutSeconds() == null
                ? 60 : serverConfig.getTimeoutSeconds());

        McpTransport transport = buildTransport(serverConfig, connection, auth, timeout);
        log.debug("创建 MCP 客户端: serverId={}, transportType={}", serverConfig.getId(), serverConfig.getTransportType());
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

    /**
     * 根据传输类型构建对应的 MCP Transport 实例。
     *
     * @param serverConfig MCP 服务配置
     * @param connection   解析后的连接配置 Map
     * @param auth         解析后的鉴权配置 Map
     * @param timeout      超时时长
     * @return MCP Transport 实例
     */
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

    /**
     * 构建 STDIO 类型 MCP Transport，从连接配置中提取命令行和环境变量。
     *
     * @param connection 解析后的连接配置，必须包含 command 数组
     * @return STDIO Transport 实例
     */
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

    /**
     * 构建 HTTP 类型 MCP Transport，支持自定义 Header 和 Bearer Token 鉴权。
     *
     * @param connection 解析后的连接配置，必须包含 url 字段
     * @param auth       解析后的鉴权配置
     * @param timeout    超时时长
     * @return HTTP Transport 实例
     */
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

    /**
     * 合并连接配置和鉴权配置中的 HTTP Headers，优先级：鉴权 Headers 覆盖连接 Headers。
     *
     * @param connection 连接配置，可能包含 headers 字段
     * @param auth       鉴权配置，可能包含 headers 和 bearerToken 字段
     * @return 合并后的 Headers Map
     */
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
