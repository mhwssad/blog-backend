package com.cybzacg.blogbackend.module.chat.shared.support;

import com.cybzacg.blogbackend.dto.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsConversationUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMembersUpdatedPayload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天 WebSocket 推送载荷构造共享组件。
 */
@Component
public class ChatPushPayloadBuilder {

    public ChatWsConversationUpdatedPayload buildConversationUpdatedPayload(String action,
                                                                             ChatConversation conversation,
                                                                             List<ChatConversationMember> activeMembers) {
        return ChatWsConversationUpdatedPayload.builder()
                .action(action)
                .conversationId(conversation.getId())
                .conversationType(conversation.getConversationType())
                .name(conversation.getName())
                .avatar(conversation.getAvatar())
                .ownerId(conversation.getOwnerId())
                .notice(conversation.getAnnouncement())
                .status(conversation.getStatus())
                .memberCount((long) (activeMembers == null ? 0 : activeMembers.size()))
                .build();
    }

    public ChatWsMembersUpdatedPayload buildMembersUpdatedPayload(String action,
                                                                   Long conversationId,
                                                                   Long affectedUserId,
                                                                   List<ChatMemberVO> members) {
        return ChatWsMembersUpdatedPayload.builder()
                .action(action)
                .conversationId(conversationId)
                .affectedUserId(affectedUserId)
                .members(members)
                .build();
    }
}
