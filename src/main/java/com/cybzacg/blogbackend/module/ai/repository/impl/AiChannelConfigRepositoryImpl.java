package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.AiChannelConfig;
import com.cybzacg.blogbackend.mapper.AiChannelConfigMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiChannelConfigRepository;
import org.springframework.stereotype.Repository;

/**
 * AiChannelConfig Repository 实现。
 */
@Repository
public class AiChannelConfigRepositoryImpl extends ServiceImpl<AiChannelConfigMapper, AiChannelConfig>
        implements AiChannelConfigRepository {
}
