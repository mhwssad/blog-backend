package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.AiUsageLog;
import com.cybzacg.blogbackend.mapper.AiUsageLogMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiUsageLogRepository;
import org.springframework.stereotype.Repository;

/**
 * AiUsageLog Repository 实现。
 */
@Repository
public class AiUsageLogRepositoryImpl extends ServiceImpl<AiUsageLogMapper, AiUsageLog>
        implements AiUsageLogRepository {
}
