package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentTaskLog;
import com.cybzacg.blogbackend.dto.mapper.ai.AiAgentTaskLogMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentTaskLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AiAgentTaskLog Repository 实现。
 */
@Repository
public class AiAgentTaskLogRepositoryImpl
        extends ServiceImpl<AiAgentTaskLogMapper, AiAgentTaskLog>
        implements AiAgentTaskLogRepository {

    @Override
    public List<AiAgentTaskLog> listByTaskId(Long taskId) {
        return list(new LambdaQueryWrapper<AiAgentTaskLog>()
                .eq(AiAgentTaskLog::getTaskId, taskId)
                .orderByAsc(AiAgentTaskLog::getTurnIndex));
    }
}
