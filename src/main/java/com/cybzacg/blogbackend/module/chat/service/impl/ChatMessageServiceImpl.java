package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.mapper.ChatMessageMapper;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageService;
import org.springframework.stereotype.Service;

/**
 * 聊天消息基础服务实现。
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {
}
