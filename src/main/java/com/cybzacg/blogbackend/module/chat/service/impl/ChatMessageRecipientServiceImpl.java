package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatMessageRecipient;
import com.cybzacg.blogbackend.mapper.ChatMessageRecipientMapper;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageRecipientService;
import org.springframework.stereotype.Service;

/**
 * 聊天消息接收状态基础服务实现。
 */
@Service
public class ChatMessageRecipientServiceImpl extends ServiceImpl<ChatMessageRecipientMapper, ChatMessageRecipient>
        implements ChatMessageRecipientService {
}
