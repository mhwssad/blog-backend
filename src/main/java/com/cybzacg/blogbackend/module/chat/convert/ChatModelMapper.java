package com.cybzacg.blogbackend.module.chat.convert;

import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.domain.ChatChannelCreateApplication;
import com.cybzacg.blogbackend.domain.ChatGroupJoinApplication;
import com.cybzacg.blogbackend.domain.ChatGroupInviteLink;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.domain.ChatMessageReadCursor;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatChannelApplicationAdminVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.module.chat.model.user.*;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

/**
 * 聊天模块对象转换。
 */
@Mapper(componentModel = "spring", imports = StrUtils.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatModelMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "allSite", expression = "java(item.getIsAllSite() != null && item.getIsAllSite() == 1)")
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "targetUserId", ignore = true)
    @Mapping(target = "targetUsername", ignore = true)
    @Mapping(target = "targetNickname", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    ChatConversationVO toConversationVO(ChatConversationListItem item);

    @Mapping(target = "allSite", expression = "java(item.getIsAllSite() != null && item.getIsAllSite() == 1)")
    @Mapping(target = "ownerUsername", ignore = true)
    @Mapping(target = "ownerNickname", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    ChatAdminConversationVO toAdminConversationVO(ChatAdminConversationListItem item);

    @Mapping(target = "joined", expression = "java(item.getSelfRole() != null)")
    ChatGroupSearchVO toGroupSearchVO(ChatConversationListItem item);

    @Mapping(target = "senderNickname", ignore = true)
    @Mapping(target = "messageType", ignore = true)
    @Mapping(target = "content", ignore = true)
    ChatConversationLastMessageVO toConversationLastMessageVO(ChatConversationListItem item);

    @Mapping(target = "senderNickname", ignore = true)
    @Mapping(target = "messageType", ignore = true)
    @Mapping(target = "content", ignore = true)
    ChatConversationLastMessageVO toConversationLastMessageVO(ChatAdminConversationListItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conversationType", constant = "group")
    @Mapping(target = "name", expression = "java(StrUtils.trim(request.getName()))")
    @Mapping(target = "avatar", expression = "java(StrUtils.trimToNull(request.getAvatar()))")
    @Mapping(target = "sceneType", constant = "user_group")
    @Mapping(target = "visibilityScope", expression = "java(StrUtils.trimToNull(request.getVisibilityScope()))")
    @Mapping(target = "allowGuestView", constant = "0")
    @Mapping(target = "requireJoinToSpeak", constant = "1")
    @Mapping(target = "joinRule", expression = "java(StrUtils.trimToNull(request.getJoinRule()))")
    @Mapping(target = "speakLevelLimit", source = "speakLevelLimit")
    @Mapping(target = "memberLimit", source = "memberLimit")
    @Mapping(target = "remark", expression = "java(StrUtils.trimToNull(request.getDescription()))")
    @Mapping(target = "announcement", expression = "java(StrUtils.trimToNull(request.getAnnouncement()))")
    @Mapping(target = "slowModeSeconds", constant = "0")
    @Mapping(target = "displaySort", constant = "0")
    @Mapping(target = "channelCategoryCode", expression = "java(StrUtils.trimToNull(request.getCategoryCode()))")
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "singlePairKey", ignore = true)
    @Mapping(target = "isAllSite", constant = "0")
    @Mapping(target = "allSiteKey", ignore = true)
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "lastMessageId", ignore = true)
    @Mapping(target = "lastMessageTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChatConversation toGroupConversation(ChatCreateGroupRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicantUserId", ignore = true)
    @Mapping(target = "desiredName", expression = "java(StrUtils.trim(request.getDesiredName()))")
    @Mapping(target = "desiredSceneType", expression = "java(StrUtils.trimToNull(request.getDesiredSceneType()))")
    @Mapping(target = "desiredCategoryCode", expression = "java(StrUtils.trimToNull(request.getDesiredCategoryCode()))")
    @Mapping(target = "description", expression = "java(StrUtils.trimToNull(request.getDescription()))")
    @Mapping(target = "applyStatus", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "reviewerId", ignore = true)
    @Mapping(target = "reviewComment", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChatChannelCreateApplication toChannelApplication(ChatChannelApplicationSubmitRequest request);

    @Mapping(target = "applyStatusLabel", ignore = true)
    ChatChannelApplicationVO toChannelApplicationVO(ChatChannelCreateApplication application);

    @Mapping(target = "applicantUsername", ignore = true)
    @Mapping(target = "applicantNickname", ignore = true)
    @Mapping(target = "applicantAvatar", ignore = true)
    @Mapping(target = "reviewerUsername", ignore = true)
    @Mapping(target = "reviewerNickname", ignore = true)
    @Mapping(target = "applyStatusLabel", ignore = true)
    ChatChannelApplicationAdminVO toChannelApplicationAdminVO(ChatChannelCreateApplication application);

    @Mapping(target = "applicantUsername", ignore = true)
    @Mapping(target = "applicantNickname", ignore = true)
    @Mapping(target = "applicantAvatar", ignore = true)
    @Mapping(target = "reviewerUsername", ignore = true)
    @Mapping(target = "reviewerNickname", ignore = true)
    @Mapping(target = "applyStatusLabel", ignore = true)
    ChatGroupJoinApplicationVO toGroupJoinApplicationVO(ChatGroupJoinApplication application);

    @Mapping(target = "expired", ignore = true)
    @Mapping(target = "usageExhausted", ignore = true)
    ChatGroupInviteLinkVO toGroupInviteLinkVO(ChatGroupInviteLink inviteLink);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "messageType", constant = "text")
    @Mapping(target = "content", expression = "java(StrUtils.trim(request.getContent()))")
    @Mapping(target = "payloadJson", ignore = true)
    @Mapping(target = "replyMessageId", ignore = true)
    @Mapping(target = "mentionAll", constant = "0")
    @Mapping(target = "mentionedUserIds", ignore = true)
    @Mapping(target = "sendStatus", constant = "1")
    @Mapping(target = "revokeStatus", constant = "0")
    @Mapping(target = "revokedBy", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    @Mapping(target = "clientMessageId", expression = "java(StrUtils.trimToNull(request.getClientMessageId()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChatMessage toTextMessage(ChatSendTextRequest request);

    @Mapping(target = "role", source = "memberRole")
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "nickname", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    ChatMemberVO toMemberVO(ChatConversationMember member);

    @Mapping(target = "senderUsername", ignore = true)
    @Mapping(target = "senderNickname", ignore = true)
    @Mapping(target = "senderAvatar", ignore = true)
    @Mapping(target = "self", ignore = true)
    @Mapping(target = "readByCurrentUser", ignore = true)
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "reply", ignore = true)
    @Mapping(target = "revoked", ignore = true)
    @Mapping(target = "edited", ignore = true)
    ChatMessageVO toMessageVO(ChatMessageHistoryItem item);

    @Mapping(target = "senderUsername", ignore = true)
    @Mapping(target = "senderNickname", ignore = true)
    @Mapping(target = "senderAvatar", ignore = true)
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "reply", ignore = true)
    @Mapping(target = "edited", ignore = true)
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
