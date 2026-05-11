package com.cybzacg.blogbackend.dto.repository.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.mapper.ai.AiChannelConfigMapper;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AiChannelConfig Repository 实现。
 */
@Repository
public class AiChannelConfigRepositoryImpl extends ServiceImpl<AiChannelConfigMapper, AiChannelConfig>
        implements AiChannelConfigRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public AiChannelConfig findByChannelCode(String channelCode) {
        return getOne(new LambdaQueryWrapper<AiChannelConfig>()
                .eq(AiChannelConfig::getChannelCode, channelCode)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AiChannelConfig> listEnabledOrderByDefault() {
        return list(new LambdaQueryWrapper<AiChannelConfig>()
                .eq(AiChannelConfig::getStatus, 1)
                .orderByDesc(AiChannelConfig::getIsDefault)
                .orderByAsc(AiChannelConfig::getId));
    }
}
