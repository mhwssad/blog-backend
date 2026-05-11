package com.cybzacg.blogbackend.dto.repository.ai.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiToolCallLog;
import com.cybzacg.blogbackend.dto.mapper.ai.AiToolCallLogMapper;
import com.cybzacg.blogbackend.dto.repository.ai.AiToolCallLogRepository;
import org.springframework.stereotype.Repository;

/**
 * AI 工具调用日志 Repository 实现。
 */
@Repository
public class AiToolCallLogRepositoryImpl extends ServiceImpl<AiToolCallLogMapper, AiToolCallLog>
        implements AiToolCallLogRepository {
}
