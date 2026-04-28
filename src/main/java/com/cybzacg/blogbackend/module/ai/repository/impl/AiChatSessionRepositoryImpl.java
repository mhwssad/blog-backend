package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.AiChatSession;
import com.cybzacg.blogbackend.mapper.AiChatSessionMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiChatSessionRepository;
import org.springframework.stereotype.Repository;

/**
 * AiChatSession Repository 实现。
 */
@Repository
public class AiChatSessionRepositoryImpl extends ServiceImpl<AiChatSessionMapper, AiChatSession>
        implements AiChatSessionRepository {
}
