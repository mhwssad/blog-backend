package com.cybzacg.blogbackend.module.auth.account.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户更新公开资料请求。
 */
@Data
@Schema(description = "更新公开资料请求")
public class UserProfileUpdateRequest {
    @Size(max = 50, message = "昵称最多50个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Size(max = 500, message = "头像URL最多500个字符")
    @Schema(description = "头像URL")
    private String avatar;

    @Size(max = 500, message = "个人简介最多500个字符")
    @Schema(description = "个人简介")
    private String bio;

    @Pattern(regexp = "^$|^https?://.*", message = "个人站点必须是合法的 HTTP/HTTPS URL")
    @Size(max = 255, message = "个人站点最多255个字符")
    @Schema(description = "个人站点")
    private String website;

    @Schema(description = "性别：0-未知，1-男，2-女，3-保密")
    private Integer gender;
}
