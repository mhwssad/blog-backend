package com.cybzacg.blogbackend.module.chat.member.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户频道创建申请分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户频道创建申请分页查询条件")
public class ChatChannelApplicationPageQuery extends PageQuery {

}
