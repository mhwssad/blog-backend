package com.cybzacg.blogbackend.module.chat.shared.convert;

import com.cybzacg.blogbackend.domain.chat.*;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationLastMessageVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatCreateGroupRequest;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupSearchVO;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatChannelApplicationAdminVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.*;
import com.cybzacg.blogbackend.module.chat.message.model.admin.ChatAdminMessageVO;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatSendTextRequest;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatReplyMessageVO;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

/**
 * 聊天模块对象转换。
 */
@Mapper(componentModel = "spring", imports = StrUtils.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatModelConvert {
    @Mapping(target = "allSite", expression = "java(item.getIsAllSite() != null && item.getIsAllSite() == 1)")
    @Mapping(target = "memberCount", ignore = true)
    ChatConversationVO toConversationVO(ChatConversationListItem item);

    @Mapping(target = "allSite", expression = "java(item.getIsAllSite() != null && item.getIsAllSite() == 1)")
    ChatAdminConversationVO toAdminConversationVO(ChatAdminConversationListItem item);

    @Mapping(target = "joined", expression = "java(item.getSelfRole() != null)")
    ChatGroupSearchVO toGroupSearchVO(ChatConversationListItem item);

    ChatConversationLastMessageVO toConversationLastMessageVO(ChatConversationListItem item);

    ChatConversationLastMessageVO toConversationLastMessageVO(ChatAdminConversationListItem item);

    @Mapping(target = "conversationType", constant = "group")
    @Mapping(target = "name", expression = "java(StrUtils.trim(request.getName()))")
    @Mapping(target = "avatar", expression = "java(StrUtils.trimToNull(request.getAvatar()))")
    @Mapping(target = "sceneType", constant = "user_group")
    @Mapping(target = "visibilityScope", expression = "java(StrUtils.trimToNull(request.getVisibilityScope()))")
    @Mapping(target = "allowGuestView", constant = "0")
    @Mapping(target = "requireJoinToSpeak", constant = "1")
    @Mapping(target = "joinRule", expression = "java(StrUtils.trimToNull(request.getJoinRule()))")
    @Mapping(target = "remark", expression = "java(StrUtils.trimToNull(request.getDescription()))")
    @Mapping(target = "announcement", expression = "java(StrUtils.trimToNull(request.getAnnouncement()))")
    @Mapping(target = "slowModeSeconds", constant = "0")
    @Mapping(target = "displaySort", constant = "0")
    @Mapping(target = "channelCategoryCode", expression = "java(StrUtils.trimToNull(request.getCategoryCode()))")
    @Mapping(target = "isAllSite", constant = "0")
    @Mapping(target = "status", constant = "1")
    ChatConversation toGroupConversation(ChatCreateGroupRequest request);

    @Mapping(target = "desiredName", expression = "java(StrUtils.trim(request.getDesiredName()))")
    @Mapping(target = "desiredSceneType", expression = "java(StrUtils.trimToNull(request.getDesiredSceneType()))")
    @Mapping(target = "desiredCategoryCode", expression = "java(StrUtils.trimToNull(request.getDesiredCategoryCode()))")
    @Mapping(target = "description", expression = "java(StrUtils.trimToNull(request.getDescription()))")
    ChatChannelCreateApplication toChannelApplication(ChatChannelApplicationSubmitRequest request);

    ChatChannelApplicationVO toChannelApplicationVO(ChatChannelCreateApplication application);

    ChatChannelApplicationAdminVO toChannelApplicationAdminVO(ChatChannelCreateApplication application);

    ChatGroupJoinApplicationVO toGroupJoinApplicationVO(ChatGroupJoinApplication application);

    ChatGroupInviteLinkVO toGroupInviteLinkVO(ChatGroupInviteLink inviteLink);

    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "messageType", constant = "text")
    @Mapping(target = "content", expression = "java(StrUtils.trim(request.getContent()))")
    @Mapping(target = "replyMessageId", ignore = true)
    @Mapping(target = "mentionAll", constant = "0")
    @Mapping(target = "sendStatus", constant = "1")
    @Mapping(target = "revokeStatus", constant = "0")
    @Mapping(target = "clientMessageId", expression = "java(StrUtils.trimToNull(request.getClientMessageId()))")
    ChatMessage toTextMessage(ChatSendTextRequest request);

    @Mapping(target = "role", source = "memberRole")
    ChatMemberVO toMemberVO(ChatConversationMember member);

    ChatMessageVO toMessageVO(ChatMessageHistoryItem item);

    ChatAdminMessageVO toAdminMessageVO(ChatAdminMessageItem item);

    ChatReadStateVO toReadStateVO(ChatMessageReadCursor cursor);

    default ChatConversationMember toConversationMember(Long conversationId,
                                                        Long userId,
                                                        String memberRole,
                                                        String joinSource,
                                                        Long lastMessageId,
                                                        LocalDateTime lastMessageTime) {
        ChatConversationMember member = new ChatConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setMemberRole(memberRole);
        member.setJoinSource(joinSource);
        member.setStatus(1);
        member.setJoinedAt(LocalDateTime.now());
        member.setLastReadMessageId(lastMessageId);
        member.setLastReadAt(lastMessageTime);
        member.setLastDeliveredMessageId(lastMessageId);
        member.setLastDeliveredAt(lastMessageTime);
        return member;
    }

    default ChatMessagePayloadVO toMessagePayloadVO(ChatFilePayloadVO filePayload) {
        ChatMessagePayloadVO payload = new ChatMessagePayloadVO();
        payload.setFile(filePayload);
        return payload;
    }

    default ChatMessagePayloadVO toMessagePayloadVO(ChatFilePayloadVO filePayload, ChatReplyMessageVO replySnapshot) {
        ChatMessagePayloadVO payload = new ChatMessagePayloadVO();
        payload.setFile(filePayload);
        payload.setReply(replySnapshot);
        return payload;
    }
}
