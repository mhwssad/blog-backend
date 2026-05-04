package com.cybzacg.blogbackend.module.chat.governance.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 禁言记录分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "禁言记录分页查询")
public class ChatMutePageQuery extends PageQuery {
    @Schema(description = "被禁言用户ID")
    private Long userId;

    @Schema(description = "禁言范围：global/lobby/topic_channel/group")
    private String scope;

    @Schema(description = "状态：0-已解除 1-生效中")
    private Integer status;
}
