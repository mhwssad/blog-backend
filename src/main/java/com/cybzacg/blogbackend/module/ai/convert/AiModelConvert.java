package com.cybzacg.blogbackend.module.ai.convert;

import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.domain.ai.AiUsageLog;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionDetailVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionVO;
import org.mapstruct.*;

/**
 * AI模块对象转换。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiModelConvert {

    AiSessionVO toSessionVO(AiChatSession session);

    AiSessionDetailVO toSessionDetailVO(AiChatSession session);

    AiMessageVO toMessageVO(AiChatMessage message);

    AiChannelConfigVO toChannelConfigVO(AiChannelConfig config);

    AiChannelConfig toChannelConfig(AiChannelConfigSaveRequest request);

    @InheritConfiguration
    void updateChannelConfig(AiChannelConfigSaveRequest request, @MappingTarget AiChannelConfig config);

    AiUsageLogVO toUsageLogVO(AiUsageLog log);
}
