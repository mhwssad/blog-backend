package com.cybzacg.blogbackend.module.auth.notice.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "我的通知分页查询条件")
public class UserNoticePageQuery extends PageQuery {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "读取状�?0-未读 1-已读")
    private Integer isRead;
}
