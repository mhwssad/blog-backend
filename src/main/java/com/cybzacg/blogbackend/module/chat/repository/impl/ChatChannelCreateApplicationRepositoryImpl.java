package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatChannelCreateApplication;
import com.cybzacg.blogbackend.mapper.ChatChannelCreateApplicationMapper;
import com.cybzacg.blogbackend.module.chat.repository.ChatChannelCreateApplicationRepository;
import org.springframework.stereotype.Repository;

/**
 * 频道创建申请 Repository 实现。
 */
@Repository
public class ChatChannelCreateApplicationRepositoryImpl
        extends ServiceImpl<ChatChannelCreateApplicationMapper, ChatChannelCreateApplication>
        implements ChatChannelCreateApplicationRepository {
}
