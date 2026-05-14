package com.cybzacg.blogbackend.module.content.friendlink.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "友情链接新增/修改请求")
public class FriendLinkSaveRequest {
    @NotBlank(message = "站点名称不能为空")
    @Size(max = 64, message = "站点名称最长64字符")
    @Schema(description = "站点名称")
    private String name;

    @NotBlank(message = "站点地址不能为空")
    @Pattern(regexp = "^https?://.+", message = "站点地址必须以 http:// 或 https:// 开头")
    @Schema(description = "站点地址")
    private String url;

    @Pattern(regexp = "^(https?://.*)?$", message = "图标地址必须以 http:// 或 https:// 开头")
    @Schema(description = "图标地址")
    private String iconUrl;

    @Size(max = 255, message = "描述最长255字符")
    @Schema(description = "站点描述")
    private String description;

    @Min(value = 0, message = "排序值不能为负数")
    @Schema(description = "排序值")
    private Integer sortOrder;
}
