package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiToolCallLog;
import com.cybzacg.blogbackend.mapper.ai.AiToolCallLogMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiToolCallLogRepository;
import org.springframework.stereotype.Repository;

/**
 * AI 工具调用日志 Repository 实现。
 */
@Repository
public class AiToolCallLogRepositoryImpl extends ServiceImpl<AiToolCallLogMapper, AiToolCallLog>
        implements AiToolCallLogRepository {
}
