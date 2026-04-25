package com.cybzacg.blogbackend.config.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 配置属性。<p>映射 {@code websocket} 前缀下的握手端点、跨域来源模式和令牌查询参数名。</p>
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {
    /**
     * WebSocket 握手端点。
     */
    @NotBlank
    private String endpoint = "/ws/chat";

    /**
     * 允许跨域的来源模式。
     */
    @NotEmpty
    private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));

    /**
     * 原生 WebSocket 握手时读取访问令牌的查询参数名。
     */
    @NotBlank
    private String tokenQueryParam = "accessToken";
}
