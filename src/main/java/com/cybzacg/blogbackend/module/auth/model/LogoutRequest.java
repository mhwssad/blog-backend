package com.cybzacg.blogbackend.module.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "退出登录请求")
public class LogoutRequest {

    @Schema(description = "访问令牌，不传时默认读取 Authorization 请求头")
    private String accessToken;
}
