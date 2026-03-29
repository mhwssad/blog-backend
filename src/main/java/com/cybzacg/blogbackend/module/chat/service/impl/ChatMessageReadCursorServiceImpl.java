package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatMessageReadCursor;
import com.cybzacg.blogbackend.mapper.ChatMessageReadCursorMapper;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageReadCursorService;
import org.springframework.stereotype.Service;

/**
 * 聊天会话已读游标基础服务实现。
 */
@Service
public class ChatMessageReadCursorServiceImpl extends ServiceImpl<ChatMessageReadCursorMapper, ChatMessageReadCursor>
        implements ChatMessageReadCursorService {
}
