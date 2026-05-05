package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiMcpToolSnapshot;
import com.cybzacg.blogbackend.mapper.ai.AiMcpToolSnapshotMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiMcpToolSnapshotRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MCP 工具快照 Repository 实现。
 */
@Repository
public class AiMcpToolSnapshotRepositoryImpl extends ServiceImpl<AiMcpToolSnapshotMapper, AiMcpToolSnapshot>
        implements AiMcpToolSnapshotRepository {

    @Override
    public List<AiMcpToolSnapshot> listByServerId(Long serverId) {
        return list(new LambdaQueryWrapper<AiMcpToolSnapshot>()
                .eq(AiMcpToolSnapshot::getMcpServerId, serverId)
                .orderByAsc(AiMcpToolSnapshot::getId));
    }
}
