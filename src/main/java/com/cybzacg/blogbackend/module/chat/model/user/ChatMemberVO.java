package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * 群成员信息。
 */
@Data
@Schema(description = "群成员信息")
public class ChatMemberVO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "成员角色")
    private String role;

    @Schema(description = "成员状态")
    private Integer status;

    @Schema(description = "加入时间")
    private Date joinedAt;

    @Schema(description = "禁言截止时间")
    private Date muteUntil;
}
