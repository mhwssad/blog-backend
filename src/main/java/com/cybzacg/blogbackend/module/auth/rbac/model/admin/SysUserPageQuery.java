package com.cybzacg.blogbackend.module.auth.rbac.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页查询条件")
public class SysUserPageQuery extends PageQuery {

    @Schema(description = "用户")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机")
    private String phone;

    @Schema(description = "状态")
    private Integer status;
}
