package com.cybzacg.blogbackend.dto.repository.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiToolAuthorization;
import com.cybzacg.blogbackend.dto.mapper.ai.AiToolAuthorizationMapper;
import com.cybzacg.blogbackend.dto.repository.ai.AiToolAuthorizationRepository;
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
