package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.mapper.ChatConversationMapper;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationService;
import org.springframework.stereotype.Service;

/**
 * 聊天会话基础服务实现。
 */
@Service
public class ChatConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversation>
        implements ChatConversationService {
}
