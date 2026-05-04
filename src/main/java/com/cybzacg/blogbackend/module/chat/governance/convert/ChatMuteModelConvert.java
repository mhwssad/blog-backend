package com.cybzacg.blogbackend.module.chat.governance.convert;

import com.cybzacg.blogbackend.domain.chat.ChatUserMuteRecord;
import com.cybzacg.blogbackend.enums.chat.ChatMuteRecordStatusEnum;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMuteCreateRequest;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMuteRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 禁言模块对象转换。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMuteModelConvert {

    @Mapping(target = "status", expression = "java(com.cybzacg.blogbackend.enums.chat.ChatMuteRecordStatusEnum.ACTIVE.getValue())")
    @Mapping(target = "sourceType", constant = "admin")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "releasedBy", ignore = true)
    @Mapping(target = "releasedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChatUserMuteRecord toEntity(ChatMuteCreateRequest request, Long operatorId);

    @Mapping(target = "username", ignore = true)
    @Mapping(target = "nickname", ignore = true)
    @Mapping(target = "conversationName", ignore = true)
    @Mapping(target = "operatorUsername", ignore = true)
    ChatMuteRecordVO toRecordVO(ChatUserMuteRecord record);
}
