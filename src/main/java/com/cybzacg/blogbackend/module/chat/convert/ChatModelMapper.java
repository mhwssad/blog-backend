package com.cybzacg.blogbackend.module.chat.convert;

import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.domain.ChatMessageReadCursor;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageVO;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationLastMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatCreateGroupRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest;
import com.cybzacg.blogbackend.utils.StrUtils;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 聊天模块对象转换。
 */
@Mapper(componentModel = "spring", imports = StrUtils.class)
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

    @Mapping(target = "senderNickname", ignore = true)
    ChatConversationLastMessageVO toConversationLastMessageVO(ChatConversationListItem item);

    @Mapping(target = "senderNickname", ignore = true)
    ChatConversationLastMessageVO toConversationLastMessageVO(ChatAdminConversationListItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conversationType", constant = "group")
    @Mapping(target = "name", expression = "java(StrUtils.trim(request.getName()))")
    @Mapping(target = "avatar", expression = "java(StrUtils.trimToNull(request.getAvatar()))")
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "singlePairKey", ignore = true)
    @Mapping(target = "isAllSite", constant = "0")
    @Mapping(target = "allSiteKey", ignore = true)
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "remark", ignore = true)
    @Mapping(target = "lastMessageId", ignore = true)
    @Mapping(target = "lastMessageTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChatConversation toGroupConversation(ChatCreateGroupRequest request);

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
    ChatMemberVO toMemberVO(ChatConversationMember member);

    @Mapping(target = "senderUsername", ignore = true)
    @Mapping(target = "senderNickname", ignore = true)
    @Mapping(target = "senderAvatar", ignore = true)
    @Mapping(target = "self", ignore = true)
    @Mapping(target = "readByCurrentUser", ignore = true)
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "revoked", ignore = true)
    @Mapping(target = "edited", ignore = true)
    ChatMessageVO toMessageVO(ChatMessageHistoryItem item);

    @Mapping(target = "senderUsername", ignore = true)
    @Mapping(target = "senderNickname", ignore = true)
    @Mapping(target = "senderAvatar", ignore = true)
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "edited", ignore = true)
    ChatAdminMessageVO toAdminMessageVO(ChatAdminMessageItem item);

    ChatReadStateVO toReadStateVO(ChatMessageReadCursor cursor);

    default ChatConversationMember toConversationMember(Long conversationId,
                                                        Long userId,
                                                        String memberRole,
                                                        String joinSource,
                                                        Long lastMessageId,
                                                        Date lastMessageTime) {
        ChatConversationMember member = new ChatConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setMemberRole(memberRole);
        member.setJoinSource(joinSource);
        member.setStatus(1);
        member.setJoinedAt(new Date());
        member.setLastReadMessageId(lastMessageId);
        member.setLastReadAt(lastMessageTime);
        member.setLastDeliveredMessageId(lastMessageId);
        member.setLastDeliveredAt(lastMessageTime);
        return member;
    }
}
