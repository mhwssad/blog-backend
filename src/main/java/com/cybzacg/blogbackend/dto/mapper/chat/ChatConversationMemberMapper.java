package com.cybzacg.blogbackend.dto.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversationMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话成员 Mapper。
 */
@Mapper
public interface ChatConversationMemberMapper
    extends BaseMapper<ChatConversationMember> {}
