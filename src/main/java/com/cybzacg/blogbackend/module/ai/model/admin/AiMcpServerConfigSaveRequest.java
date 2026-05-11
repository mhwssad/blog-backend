package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.core.validation.ValidJsonObject;
import com.cybzacg.blogbackend.enums.ai.AiMcpTransportTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * MCP 服务配置创建/更新请求。
 */
@Data
@Schema(description = "MCP 服务配置创建/更新请求")
public class AiMcpServerConfigSaveRequest {
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 128, message = "服务名称最多128个字符")
    @Schema(description = "服务名称")
    private String serverName;

    @NotBlank(message = "传输类型不能为空")
    @EnumValue(enumClass = AiMcpTransportTypeEnum.class, method = "getCode", message = "传输类型无效")
    @Schema(description = "传输类型 stdio/http")
    private String transportType;

    @NotBlank(message = "连接配置不能为空")
    @ValidJsonObject(message = "连接配置必须是 JSON 对象")
    @Schema(description = "连接配置 JSON")
    private String connectionConfigJson;

    @ValidJsonObject(message = "鉴权配置必须是 JSON 对象")
    @Schema(description = "鉴权配置 JSON")
    private String authConfigJson;

    @NotNull(message = "超时时间不能为空")
    @Min(value = 1, message = "超时时间必须大于 0")
    @Schema(description = "超时时间（秒）")
    private Integer timeoutSeconds;

    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "启用状态必须为 0 或 1")
    @Max(value = 1, message = "启用状态必须为 0 或 1")
    @Schema(description = "启用状态：0-停用/1-启用")
    private Integer enabled;

    @Schema(description = "MFA 票据")
    private String mfaTicket;
}
