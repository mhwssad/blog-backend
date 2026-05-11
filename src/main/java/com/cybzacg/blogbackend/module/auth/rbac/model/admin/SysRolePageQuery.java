package com.cybzacg.blogbackend.module.auth.rbac.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色分页查询条件")
public class SysRolePageQuery extends PageQuery {

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "状态")
    private Integer status;
}
