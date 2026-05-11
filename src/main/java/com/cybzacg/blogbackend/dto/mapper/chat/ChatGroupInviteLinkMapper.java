package com.cybzacg.blogbackend.dto.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.chat.ChatGroupInviteLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群聊邀请链接 Mapper。
 */
@Mapper
public interface ChatGroupInviteLinkMapper
    extends BaseMapper<ChatGroupInviteLink> {}
