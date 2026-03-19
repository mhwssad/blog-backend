package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "角色新增/修改请求")
public class SysRoleSaveRequest {
    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "数据权限")
    private Integer dataScope;
}
