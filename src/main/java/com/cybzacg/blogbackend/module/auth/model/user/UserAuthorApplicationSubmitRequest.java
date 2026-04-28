package com.cybzacg.blogbackend.module.auth.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户提交作者申请请求。
 */
@Data
@Schema(description = "用户提交作者申请请求")
public class UserAuthorApplicationSubmitRequest {
    @NotBlank(message = "申请说明不能为空")
    @Size(max = 512, message = "申请说明长度不能超过512个字符")
    @Schema(description = "申请说明")
    private String applyReason;

    @NotBlank(message = "内容方向不能为空")
    @Size(max = 128, message = "内容方向长度不能超过128个字符")
    @Schema(description = "擅长内容方向")
    private String contentDirection;

    @Size(max = 1024, message = "个人简介长度不能超过1024个字符")
    @Schema(description = "个人简介")
    private String introduction;

    @Size(max = 10, message = "示例链接数量不能超过10个")
    @Schema(description = "示例链接列表")
    private List<
            @Pattern(regexp = "^(https?://).+", message = "示例链接格式不正确")
            @Size(max = 512, message = "示例链接长度不能超过512个字符")
            String> sampleLinks;
}
