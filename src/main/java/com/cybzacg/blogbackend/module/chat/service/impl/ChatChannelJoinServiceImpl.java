package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.service.ChatChannelJoinService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.support.ChatPushPayloadBuilder;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 频道/私聊加入子服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatChannelJoinServiceImpl implements ChatChannelJoinService {

    private final ChatServiceSupport s;
    private final ChatPushService chatPushService;
    private final ChatPushPayloadBuilder chatPushPayloadBuilder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO openSingleConversation(Long userId, Long targetUserId) {
        com.cybzacg.blogbackend.domain.SysUser targetUser = s.requireActiveUser(targetUserId, true);
        ChatConversation conversation = ensureSingleConversation(userId, targetUser.getId());
        return s.getConversationVO(userId, conversation.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO joinConversation(Long userId, Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = s.getConversationRepository().getById(conversationId);
        ExceptionThrowerCore.throwBusinessIf(conversation == null
                        || !Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL),
                ResultErrorCode.ILLEGAL_ARGUMENT, "会话不存在或不可用");
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_GROUP),
                ResultErrorCode.ILLEGAL_ARGUMENT, "仅支持加入群聊或频道");
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(conversation.getJoinRule(), ChatConstants.JOIN_RULE_FREE),
                ResultErrorCode.FORBIDDEN, "该会话不支持直接加入，请通过申请或邀请链接");
        if (conversation.getMemberLimit() != null && conversation.getMemberLimit() > 0) {
            long currentMembers = s.getConversationMemberRepository().countActiveByConversationId(conversationId);
            ExceptionThrowerCore.throwBusinessIf(currentMembers >= conversation.getMemberLimit(),
                    ResultErrorCode.FORBIDDEN, "该会话成员已满");
        }
        s.upsertConversationMembership(conversation, userId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        List<ChatConversationMember> activeMembers = s.listActiveMembers(conversationId);
        List<ChatMemberVO> memberRecords = s.buildMemberRecords(activeMembers);
        chatPushService.pushMembersUpdated(
                chatPushPayloadBuilder.buildMembersUpdatedPayload("member_joined", conversationId, userId, memberRecords),
                activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList());
        return s.getConversationVO(userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveConversation(Long userId, Long conversationId) {
        ChatServiceSupport.ConversationAccessContext context = s.requireConversationAccess(userId, conversationId);
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(context.selfMember().getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER),
                ResultErrorCode.UNSUPPORTED_OPERATION, "群主不能直接退出，请先解散或转让");
        List<Long> notifyUserIds = context.activeUserIds();
        context.selfMember().setStatus(ChatConstants.MEMBER_STATUS_LEFT);
        s.getConversationMemberRepository().updateById(context.selfMember());
        List<ChatMemberVO> records = s.buildMemberRecords(s.listActiveMembers(conversationId));
        chatPushService.pushMembersUpdated(
                chatPushPayloadBuilder.buildMembersUpdatedPayload("member_left", conversationId, userId, records), notifyUserIds);
    }

    // ========== Private helpers ==========

    private ChatConversation ensureSingleConversation(Long userId, Long targetUserId) {
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, targetUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能给自己发送单聊消息");
        String pairKey = s.buildSinglePairKey(userId, targetUserId);
        ChatConversation conversation = s.getConversationRepository().findBySinglePairKey(pairKey);
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
            conversation.setSinglePairKey(pairKey);
            conversation.setIsAllSite(0);
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            try {
                s.getConversationRepository().save(conversation);
            } catch (DuplicateKeyException ex) {
                conversation = s.getConversationRepository().findBySinglePairKey(pairKey);
            }
        }
        ExceptionThrowerCore.throwBusinessIfNull(conversation, ResultErrorCode.ILLEGAL_ARGUMENT, "单聊会话创建失败");
        if (!Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL)) {
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            s.getConversationRepository().updateById(conversation);
        }
        s.upsertConversationMembership(conversation, userId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        s.upsertConversationMembership(conversation, targetUserId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        return conversation;
    }
}
