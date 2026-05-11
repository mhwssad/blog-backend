package com.cybzacg.blogbackend.dto.repository.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;

import java.util.List;

/**
 * AiChannelConfig Repository。
 */
public interface AiChannelConfigRepository extends IService<AiChannelConfig> {

    /**
     * 按渠道编码查询配置。
     */
    AiChannelConfig findByChannelCode(String channelCode);

    /**
     * 查询已启用渠道，默认渠道优先。
     */
    List<AiChannelConfig> listEnabledOrderByDefault();
}
