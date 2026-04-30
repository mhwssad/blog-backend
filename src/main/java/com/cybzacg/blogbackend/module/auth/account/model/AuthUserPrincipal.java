package com.cybzacg.blogbackend.module.auth.account.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.security.Principal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "认证用户主体")
public class AuthUserPrincipal implements Principal, Serializable {
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;

    @Override
    public String getName() {
        return username;
    }
}
