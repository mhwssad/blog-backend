package com.cybzacg.blogbackend.dto.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.chat.ChatMessageReadCursor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话已读游标 Mapper。
 */
@Mapper
public interface ChatMessageReadCursorMapper
    extends BaseMapper<ChatMessageReadCursor> {}
