package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.domain.ChatGroupInviteLink;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupInviteLinkCreateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupInviteLinkPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupInviteLinkVO;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatGroupInviteLinkRepository;
import com.cybzacg.blogbackend.module.chat.service.UserChatGroupInviteLinkService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * 用户侧群邀请链接服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserChatGroupInviteLinkServiceImpl implements UserChatGroupInviteLinkService {
    private static final int TOKEN_BYTES = 24;
    private static final int MAX_TOKEN_RETRY = 5;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ChatGroupInviteLinkRepository chatGroupInviteLinkRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final ChatModelMapper chatModelMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupInviteLinkVO createInviteLink(Long conversationId, ChatGroupInviteLinkCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        requireGroupManager(userId, conversationId);
        validateCreateRequest(request);
        ChatGroupInviteLink inviteLink = new ChatGroupInviteLink();
        inviteLink.setConversationId(conversationId);
        inviteLink.setCreatedBy(userId);
        inviteLink.setInviteToken(generateUniqueToken());
        inviteLink.setExpireAt(request == null ? null : request.getExpireAt());
        inviteLink.setMaxUseCount(request == null || request.getMaxUseCount() == null ? 0 : request.getMaxUseCount());
        inviteLink.setUsedCount(0);
        inviteLink.setStatus(1);
        chatGroupInviteLinkRepository.save(inviteLink);
        return toVO(inviteLink);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ChatGroupInviteLinkVO> pageInviteLinks(Long conversationId, ChatGroupInviteLinkPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        requireGroupManager(userId, conversationId);
        ChatGroupInviteLinkPageQuery safeQuery = normalizeQuery(query);
        Page<ChatGroupInviteLink> page = chatGroupInviteLinkRepository.pageByConversationId(conversationId, safeQuery);
        List<ChatGroupInviteLinkVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableInviteLink(Long conversationId, Long inviteLinkId) {
        Long userId = SecurityUtils.requireUserId();
        requireGroupManager(userId, conversationId);
        ChatGroupInviteLink inviteLink = requireInviteLink(inviteLinkId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(inviteLink.getConversationId(), conversationId),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "邀请链接不存在");
        if (!Objects.equals(inviteLink.getStatus(), 0)) {
            inviteLink.setStatus(0);
            chatGroupInviteLinkRepository.updateById(inviteLink);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinByInviteToken(String inviteToken) {
        Long userId = SecurityUtils.requireUserId();
        String token = StrUtils.trimToNull(inviteToken);
        ExceptionThrowerCore.throwBusinessIfBlank(token, ResultErrorCode.ILLEGAL_ARGUMENT, "邀请链接令牌不能为空");
        ChatGroupInviteLink inviteLink = chatGroupInviteLinkRepository.findByToken(token);
        ExceptionThrowerCore.throwBusinessIfNull(inviteLink, ResultErrorCode.ILLEGAL_ARGUMENT, "邀请链接不存在");
        ChatConversation conversation = requireNormalGroupConversation(inviteLink.getConversationId());
        ChatConversationMember existing = chatConversationMemberRepository.findByConversationAndUser(conversation.getId(), userId);
        if (existing != null && Objects.equals(existing.getStatus(), ChatConstants.MEMBER_STATUS_NORMAL)) {
            return;
        }
        validateInviteLinkUsable(inviteLink);
        ensureMemberLimitNotReached(conversation);
        upsertMember(conversation, userId);
        inviteLink.setUsedCount((inviteLink.getUsedCount() == null ? 0 : inviteLink.getUsedCount()) + 1);
        chatGroupInviteLinkRepository.updateById(inviteLink);
    }

    private void validateCreateRequest(ChatGroupInviteLinkCreateRequest request) {
        if (request == null) {
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(request.getExpireAt() != null && !request.getExpireAt().isAfter(LocalDateTime.now()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "过期时间必须晚于当前时间");
    }

    private void validateInviteLinkUsable(ChatGroupInviteLink inviteLink) {
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(inviteLink.getStatus(), 1),
                ResultErrorCode.FORBIDDEN,
                "邀请链接已停用");
        ExceptionThrowerCore.throwBusinessIf(inviteLink.getExpireAt() != null && !inviteLink.getExpireAt().isAfter(LocalDateTime.now()),
                ResultErrorCode.FORBIDDEN,
                "邀请链接已过期");
        Integer maxUseCount = inviteLink.getMaxUseCount();
        Integer usedCount = inviteLink.getUsedCount();
        ExceptionThrowerCore.throwBusinessIf(maxUseCount != null && maxUseCount > 0 && usedCount != null && usedCount >= maxUseCount,
                ResultErrorCode.FORBIDDEN,
                "邀请链接使用次数已达上限");
    }

    private ChatConversation requireGroupManager(Long userId, Long conversationId) {
        ChatConversation conversation = requireNormalGroupConversation(conversationId);
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(conversationId, userId);
        ExceptionThrowerCore.throwBusinessIf(member == null || !Objects.equals(member.getStatus(), ChatConstants.MEMBER_STATUS_NORMAL),
                ResultErrorCode.FORBIDDEN,
                "当前用户不在该群聊中");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER)
                        && !Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_ADMIN),
                ResultErrorCode.FORBIDDEN,
                "只有群主或管理员可以管理邀请链接");
        return conversation;
    }

    private ChatConversation requireNormalGroupConversation(Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIf(conversation == null
                        || !Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "群聊不存在或不可用");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_GROUP)
                        || Integer.valueOf(1).equals(conversation.getIsAllSite()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前会话不支持邀请链接");
        return conversation;
    }

    private ChatGroupInviteLink requireInviteLink(Long inviteLinkId) {
        ExceptionThrowerCore.throwBusinessIfNull(inviteLinkId, ResultErrorCode.ILLEGAL_ARGUMENT, "邀请链接ID不能为空");
        ChatGroupInviteLink inviteLink = chatGroupInviteLinkRepository.getById(inviteLinkId);
        ExceptionThrowerCore.throwBusinessIfNull(inviteLink, ResultErrorCode.ILLEGAL_ARGUMENT, "邀请链接不存在");
        return inviteLink;
    }

    private void ensureMemberLimitNotReached(ChatConversation conversation) {
        Integer memberLimit = conversation.getMemberLimit();
        if (memberLimit == null || memberLimit <= 0) {
            return;
        }
        int activeCount = chatConversationMemberRepository.listActiveByConversationId(conversation.getId()).size();
        ExceptionThrowerCore.throwBusinessIf(activeCount >= memberLimit,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前群聊人数已达上限");
    }

    private void upsertMember(ChatConversation conversation, Long userId) {
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(conversation.getId(), userId);
        if (member == null) {
            member = new ChatConversationMember();
            member.setConversationId(conversation.getId());
            member.setUserId(userId);
            member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
            member.setJoinSource(ChatConstants.JOIN_SOURCE_INVITE_LINK);
            member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
            member.setJoinedAt(LocalDateTime.now());
            member.setLastReadMessageId(conversation.getLastMessageId());
            member.setLastReadAt(conversation.getLastMessageTime());
            member.setLastDeliveredMessageId(conversation.getLastMessageId());
            member.setLastDeliveredAt(conversation.getLastMessageTime());
            chatConversationMemberRepository.save(member);
            return;
        }
        member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        member.setJoinSource(ChatConstants.JOIN_SOURCE_INVITE_LINK);
        member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        member.setMuteUntil(null);
        member.setJoinedAt(member.getJoinedAt() == null ? LocalDateTime.now() : member.getJoinedAt());
        member.setLastReadMessageId(conversation.getLastMessageId());
        member.setLastReadAt(conversation.getLastMessageTime());
        member.setLastDeliveredMessageId(conversation.getLastMessageId());
        member.setLastDeliveredAt(conversation.getLastMessageTime());
        chatConversationMemberRepository.updateById(member);
    }

    private ChatGroupInviteLinkPageQuery normalizeQuery(ChatGroupInviteLinkPageQuery query) {
        ChatGroupInviteLinkPageQuery safeQuery = query == null ? new ChatGroupInviteLinkPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE));
        return safeQuery;
    }

    private String generateUniqueToken() {
        for (int i = 0; i < MAX_TOKEN_RETRY; i++) {
            String token = generateToken();
            if (chatGroupInviteLinkRepository.findByToken(token) == null) {
                return token;
            }
        }
        throw new DuplicateKeyException("群邀请链接令牌生成失败");
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ChatGroupInviteLinkVO toVO(ChatGroupInviteLink inviteLink) {
        ChatGroupInviteLinkVO vo = chatModelMapper.toGroupInviteLinkVO(inviteLink);
        vo.setExpired(inviteLink.getExpireAt() != null && !inviteLink.getExpireAt().isAfter(LocalDateTime.now()));
        Integer maxUseCount = inviteLink.getMaxUseCount();
        Integer usedCount = inviteLink.getUsedCount();
        vo.setUsageExhausted(maxUseCount != null && maxUseCount > 0 && usedCount != null && usedCount >= maxUseCount);
        return vo;
    }
}
