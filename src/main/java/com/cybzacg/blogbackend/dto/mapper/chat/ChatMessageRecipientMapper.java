package com.cybzacg.blogbackend.dto.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.chat.ChatMessageRecipient;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息接收状态 Mapper。
 */
@Mapper
public interface ChatMessageRecipientMapper
    extends BaseMapper<ChatMessageRecipient> {}
