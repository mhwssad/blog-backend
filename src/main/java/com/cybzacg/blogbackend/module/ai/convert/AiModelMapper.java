package com.cybzacg.blogbackend.module.ai.convert;

import com.cybzacg.blogbackend.domain.AiChannelConfig;
import com.cybzacg.blogbackend.domain.AiChatMessage;
import com.cybzacg.blogbackend.domain.AiChatSession;
import com.cybzacg.blogbackend.domain.AiUsageLog;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiMessageVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionDetailVO;
import com.cybzacg.blogbackend.module.ai.model.user.AiSessionVO;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * AI模块对象转换。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiModelMapper {

    AiSessionVO toSessionVO(AiChatSession session);

    AiSessionDetailVO toSessionDetailVO(AiChatSession session);

    AiMessageVO toMessageVO(AiChatMessage message);

    AiChannelConfigVO toChannelConfigVO(AiChannelConfig config);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    AiChannelConfig toChannelConfig(AiChannelConfigSaveRequest request);

    @InheritConfiguration
    void updateChannelConfig(AiChannelConfigSaveRequest request, @MappingTarget AiChannelConfig config);

    AiUsageLogVO toUsageLogVO(AiUsageLog log);
}
