package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiToolAuthorization;
import com.cybzacg.blogbackend.mapper.ai.AiToolAuthorizationMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiToolAuthorizationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI 工具授权 Repository 实现。
 */
@Repository
public class AiToolAuthorizationRepositoryImpl extends ServiceImpl<AiToolAuthorizationMapper, AiToolAuthorization>
        implements AiToolAuthorizationRepository {

    @Override
    public List<AiToolAuthorization> listEnabledByToolId(Long toolId) {
        return list(new LambdaQueryWrapper<AiToolAuthorization>()
                .eq(AiToolAuthorization::getToolId, toolId)
                .eq(AiToolAuthorization::getEnabled, 1));
    }
}
