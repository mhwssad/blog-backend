package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.domain.chat.ChatMessage;

import java.util.Collection;

/**
 * 聊天通知编排服务。
 */
public interface ChatNotificationService {

    /**
     * 在消息发送成功后投递站内通知。
     *
     * @param conversation 会话
     * @param message      已保存的消息
     * @param senderId     发送者用户 ID
     * @param activeMembers 当前活跃成员快照
     * @param textContent  文本内容，非文本消息可为空
     */
    void deliverMessageNotifications(ChatConversation conversation,
                                     ChatMessage message,
                                     Long senderId,
                                     Collection<ChatConversationMember> activeMembers,
                                     String textContent);

    /**
     * 在主题频道公告变更后向频道成员投递通知。
     *
     * @param conversation 主题频道会话
     * @param activeMembers 当前活跃成员快照
     * @param publisherId  发布公告的用户 ID
     */
    void deliverChannelAnnouncementNotifications(ChatConversation conversation,
                                                 Collection<ChatConversationMember> activeMembers,
                                                 Long publisherId);
}
