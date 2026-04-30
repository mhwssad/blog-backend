package com.cybzacg.blogbackend.module.chat.push.service.impl;

import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.domain.chat.ChatMessage;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.push.service.ChatNotificationService;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.cybzacg.blogbackend.utils.UserDisplayNameUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 聊天通知编排服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotificationServiceImpl implements ChatNotificationService {
    private static final Pattern MENTION_USER_ID_PATTERN = Pattern.compile("(?<!\\w)@(\\d{1,19})(?!\\w)");
    private static final int CONTENT_PREVIEW_MAX_LENGTH = 80;

    private final NotificationDeliveryService notificationDeliveryService;
    private final SysUserRepository sysUserRepository;

    @Override
    public void deliverMessageNotifications(ChatConversation conversation,
                                            ChatMessage message,
                                            Long senderId,
                                            Collection<ChatConversationMember> activeMembers,
                                            String textContent) {
        if (conversation == null || message == null || senderId == null || activeMembers == null || activeMembers.isEmpty()) {
            return;
        }
        try {
            SysUser sender = sysUserRepository.getById(senderId);
            String senderName = UserDisplayNameUtils.resolveDisplayName(sender, senderId);
            deliverPrivateMessageNotice(conversation, message, senderId, activeMembers, senderName, textContent);
            deliverGroupMentionNotice(conversation, message, senderId, activeMembers, senderName, textContent);
        } catch (RuntimeException ex) {
            log.warn("deliver chat notification failed: conversationId={}, messageId={}",
                    conversation.getId(),
                    message.getId(),
                    ex);
        }
    }

    @Override
    public void deliverChannelAnnouncementNotifications(ChatConversation conversation,
                                                        Collection<ChatConversationMember> activeMembers,
                                                        Long publisherId) {
        if (conversation == null || activeMembers == null || activeMembers.isEmpty()
                || !Objects.equals(conversation.getSceneType(), ChatConstants.SCENE_TYPE_TOPIC_CHANNEL)
                || !StrUtils.hasText(conversation.getAnnouncement())) {
            return;
        }
        String conversationName = StrUtils.hasText(conversation.getName()) ? conversation.getName().trim() : "主题频道";
        String content = "「" + conversationName + "」发布了新的频道公告：" + preview(conversation.getAnnouncement());
        for (ChatConversationMember member : activeMembers) {
            Long targetUserId = member == null ? null : member.getUserId();
            if (targetUserId == null || Objects.equals(targetUserId, publisherId)) {
                continue;
            }
            notificationDeliveryService.deliverAfterCommit(
                    targetUserId,
                    NotificationTypeEnum.CHANNEL_ANNOUNCEMENT,
                    "频道公告更新",
                    content,
                    publisherId
            );
        }
    }

    private void deliverPrivateMessageNotice(ChatConversation conversation,
                                             ChatMessage message,
                                             Long senderId,
                                             Collection<ChatConversationMember> activeMembers,
                                             String senderName,
                                             String textContent) {
        if (!Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_SINGLE)) {
            return;
        }
        for (ChatConversationMember member : activeMembers) {
            Long targetUserId = member == null ? null : member.getUserId();
            if (targetUserId == null || Objects.equals(targetUserId, senderId)) {
                continue;
            }
            notificationDeliveryService.deliverAfterCommit(
                    targetUserId,
                    NotificationTypeEnum.PRIVATE_MESSAGE,
                    "收到一条私聊消息",
                    buildMessageContent(senderName, message, textContent),
                    senderId
            );
        }
    }

    private void deliverGroupMentionNotice(ChatConversation conversation,
                                           ChatMessage message,
                                           Long senderId,
                                           Collection<ChatConversationMember> activeMembers,
                                           String senderName,
                                           String textContent) {
        if (Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_SINGLE)
                || !StrUtils.hasText(textContent)) {
            return;
        }
        Set<Long> mentionedUserIds = resolveMentionedActiveUserIds(textContent, activeMembers);
        for (Long targetUserId : mentionedUserIds) {
            if (Objects.equals(targetUserId, senderId)) {
                continue;
            }
            notificationDeliveryService.deliverAfterCommit(
                    targetUserId,
                    NotificationTypeEnum.GROUP_MENTION,
                    "群聊中有人@你",
                    buildMentionContent(conversation, senderName, textContent),
                    senderId
            );
        }
    }

    private Set<Long> resolveMentionedActiveUserIds(String textContent, Collection<ChatConversationMember> activeMembers) {
        Set<Long> activeUserIds = new LinkedHashSet<>();
        for (ChatConversationMember member : activeMembers) {
            if (member != null && member.getUserId() != null) {
                activeUserIds.add(member.getUserId());
            }
        }
        Set<Long> mentionedUserIds = new LinkedHashSet<>();
        Matcher matcher = MENTION_USER_ID_PATTERN.matcher(textContent);
        while (matcher.find()) {
            Long userId = parseMentionUserId(matcher.group(1));
            if (userId != null && activeUserIds.contains(userId)) {
                mentionedUserIds.add(userId);
            }
        }
        return mentionedUserIds;
    }

    private Long parseMentionUserId(String rawUserId) {
        try {
            return Long.parseLong(rawUserId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildMessageContent(String senderName, ChatMessage message, String textContent) {
        if (Objects.equals(message.getMessageType(), ChatConstants.MESSAGE_TYPE_TEXT)) {
            return senderName + "：" + preview(textContent);
        }
        return senderName + " 发来一条" + resolveMessageTypeLabel(message.getMessageType()) + "消息";
    }

    private String buildMentionContent(ChatConversation conversation, String senderName, String textContent) {
        String conversationName = StrUtils.hasText(conversation.getName()) ? conversation.getName().trim() : "群聊";
        return senderName + " 在「" + conversationName + "」中@了你：" + preview(textContent);
    }

    private String resolveMessageTypeLabel(String messageType) {
        if (Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_IMAGE)) {
            return "图片";
        }
        if (Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_VOICE)) {
            return "语音";
        }
        return "文件";
    }

    private String preview(String textContent) {
        String normalized = StrUtils.trimToDefault(textContent, "");
        if (normalized.length() <= CONTENT_PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, CONTENT_PREVIEW_MAX_LENGTH) + "...";
    }
}
