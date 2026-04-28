package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.AiChatMessage;
import com.cybzacg.blogbackend.mapper.AiChatMessageMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiChatMessageRepository;
import org.springframework.stereotype.Repository;

/**
 * AiChatMessage Repository 实现。
 */
@Repository
public class AiChatMessageRepositoryImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage>
        implements AiChatMessageRepository {
}
