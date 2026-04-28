package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatGroupJoinApplication;
import com.cybzacg.blogbackend.mapper.ChatGroupJoinApplicationMapper;
import com.cybzacg.blogbackend.module.chat.repository.ChatGroupJoinApplicationRepository;
import org.springframework.stereotype.Repository;

/**
 * 群聊入群申请 Repository 实现。
 */
@Repository
public class ChatGroupJoinApplicationRepositoryImpl
        extends ServiceImpl<ChatGroupJoinApplicationMapper, ChatGroupJoinApplication>
        implements ChatGroupJoinApplicationRepository {
}
