package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.mapper.ChatConversationMemberMapper;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationMemberService;
import org.springframework.stereotype.Service;

/**
 * 聊天会话成员基础服务实现。
 */
@Service
public class ChatConversationMemberServiceImpl extends ServiceImpl<ChatConversationMemberMapper, ChatConversationMember>
        implements ChatConversationMemberService {
}
