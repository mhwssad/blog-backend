package com.cybzacg.blogbackend.module.chat.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.chat.ChatMessage;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatServiceSupport;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatLobbyMessageVO;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.conversation.service.ChatConversationQueryService;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.UserDisplayNameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 会话查询子服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatConversationQueryServiceImpl implements ChatConversationQueryService {

    private final ChatServiceSupport s;

    @Override
    public PageResult<ChatConversationVO> pageMyConversations(Long userId, ChatConversationPageQuery query) {
        s.ensureGlobalConversationMembership(userId);
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        String keyword = s.trimKeyword(query.getKeyword());
        long total = Objects.requireNonNullElse(s.getConversationRepository().countConversationPage(userId, keyword), 0L);
        if (total == 0L) {
            return PageResult.<ChatConversationVO>builder()
                    .total(0L)
                    .current(current)
                    .size(size)
                    .records(List.of())
                    .build();
        }
        long offset = (current - 1) * size;
        List<ChatConversationListItem> items = s.getConversationRepository().selectConversationPage(userId, keyword, offset, size);
        return PageResult.<ChatConversationVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(s.buildConversationRecords(userId, items))
                .build();
    }

    @Override
    public ChatConversationVO getMyConversation(Long userId, Long conversationId) {
        s.ensureGlobalConversationMembership(userId);
        s.requireConversationAccess(userId, conversationId);
        return s.getConversationVO(userId, conversationId);
    }

    @Override
    public PageResult<ChatLobbyMessageVO> pageLobbyMessages(Long current, Long size, Long beforeMessageId) {
        com.cybzacg.blogbackend.domain.ChatConversation conversation = s.getConversationRepository().findGlobalConversation();
        if (conversation == null) {
            conversation = new com.cybzacg.blogbackend.domain.ChatConversation();
            conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
            conversation.setName(ChatConstants.GLOBAL_CONVERSATION_NAME);
            conversation.setIsAllSite(1);
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            try {
                s.getConversationRepository().save(conversation);
            } catch (DuplicateKeyException ex) {
                conversation = s.getConversationRepository().findGlobalConversation();
            }
        }

        long currentVal = PaginationUtils.normalizeCurrent(current);
        long sizeVal = PaginationUtils.normalizeSize(size, 20L, 100L);

        LambdaQueryWrapper<ChatMessage> countWrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .eq(ChatMessage::getRevokeStatus, ChatConstants.REVOKE_STATUS_NORMAL)
                .eq(ChatMessage::getSendStatus, ChatConstants.SEND_STATUS_SENT);
        if (beforeMessageId != null) {
            countWrapper.lt(ChatMessage::getId, beforeMessageId);
        }
        long total = s.getMessageRepository().count(countWrapper);

        if (total == 0L) {
            return PageResult.<ChatLobbyMessageVO>builder()
                    .total(0L)
                    .current(currentVal)
                    .size(sizeVal)
                    .records(List.of())
                    .build();
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .eq(ChatMessage::getRevokeStatus, ChatConstants.REVOKE_STATUS_NORMAL)
                .eq(ChatMessage::getSendStatus, ChatConstants.SEND_STATUS_SENT)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + sizeVal + " OFFSET " + (currentVal - 1) * sizeVal);
        if (beforeMessageId != null) {
            queryWrapper.lt(ChatMessage::getId, beforeMessageId);
        }
        List<ChatMessage> messages = s.getMessageRepository().list(queryWrapper);

        Collections.reverse(messages);
        Set<Long> senderIds = messages.stream().map(ChatMessage::getSenderId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SysUser> userMap = s.loadUsers(senderIds);
        List<ChatLobbyMessageVO> records = messages.stream().map(msg -> {
            ChatLobbyMessageVO vo = new ChatLobbyMessageVO();
            vo.setId(msg.getId());
            vo.setSenderId(msg.getSenderId());
            SysUser sender = userMap.get(msg.getSenderId());
            vo.setSenderName(UserDisplayNameUtils.resolveDisplayName(sender, msg.getSenderId()));
            vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
            vo.setMessageType(msg.getMessageType());
            vo.setContent(msg.getContent());
            vo.setCreatedAt(msg.getCreatedAt());
            return vo;
        }).toList();
        return PageResult.<ChatLobbyMessageVO>builder()
                .total(total)
                .current(currentVal)
                .size(sizeVal)
                .records(records)
                .build();
    }
}
