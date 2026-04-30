package com.cybzacg.blogbackend.module.chat.message.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.*;
import com.cybzacg.blogbackend.module.chat.member.model.user.*;
import com.cybzacg.blogbackend.module.chat.message.model.user.*;
import com.cybzacg.blogbackend.module.chat.conversation.service.*;
import com.cybzacg.blogbackend.module.chat.member.service.*;
import com.cybzacg.blogbackend.module.chat.message.service.*;
import com.cybzacg.blogbackend.module.chat.governance.service.*;
import com.cybzacg.blogbackend.module.chat.push.service.*;
import com.cybzacg.blogbackend.module.chat.attachment.service.*;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户侧聊天服务门面。
 *
 * <p>仅做委托调用，所有业务逻辑已拆分到五个子 Service：
 * <ul>
 *     <li>{@link ChatConversationQueryService} — 会话查询</li>
 *     <li>{@link ChatMessageSendService} — 消息发送</li>
 *     <li>{@link ChatMessageLifecycleService} — 消息生命周期</li>
 *     <li>{@link ChatGroupManageService} — 群组管理</li>
 *     <li>{@link ChatChannelJoinService} — 频道/私聊加入</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserChatServiceImpl implements UserChatService {

    private final ChatConversationQueryService conversationQueryService;
    private final ChatMessageSendService messageSendService;
    private final ChatMessageLifecycleService messageLifecycleService;
    private final ChatGroupManageService groupManageService;
    private final ChatChannelJoinService channelJoinService;

    // ========== 会话查询 ==========

    @Override
    public PageResult<ChatConversationVO> pageMyConversations(ChatConversationPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        return conversationQueryService.pageMyConversations(userId, query);
    }

    @Override
    public ChatConversationVO getMyConversation(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        return conversationQueryService.getMyConversation(userId, conversationId);
    }

    // ========== 单聊打开 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO openSingleConversation(ChatOpenSingleConversationRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return channelJoinService.openSingleConversation(userId, request.getTargetUserId());
    }

    // ========== 消息历史 ==========

    @Override
    public PageResult<ChatMessageVO> pageMyMessages(Long conversationId, ChatMessagePageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        return messageLifecycleService.pageMyMessages(userId, conversationId, query);
    }

    // ========== 消息发送 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendTextMessage(ChatSendTextRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return messageSendService.sendTextMessage(userId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendTextMessage(Long userId, ChatSendTextRequest request) {
        return messageSendService.sendTextMessage(userId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendFileMessage(ChatSendFileRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return messageSendService.sendFileMessage(userId, request);
    }

    // ========== 消息生命周期 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO editMessage(Long messageId, ChatEditMessageRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return messageLifecycleService.editMessage(userId, messageId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeMessage(Long messageId) {
        Long userId = SecurityUtils.requireUserId();
        messageLifecycleService.revokeMessage(userId, messageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long messageId) {
        Long userId = SecurityUtils.requireUserId();
        messageLifecycleService.deleteMessage(userId, messageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReadStateVO markRead(Long conversationId, ChatMarkReadRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return messageLifecycleService.markRead(userId, conversationId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReadStateVO markRead(Long userId, Long conversationId, Long readMessageId) {
        return messageLifecycleService.markRead(userId, conversationId, readMessageId);
    }

    // ========== 群组管理 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO createGroup(ChatCreateGroupRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.createGroup(userId, request);
    }

    @Override
    public ChatConversationVO getGroupDetail(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.getGroupDetail(userId, conversationId);
    }

    @Override
    public List<ChatMemberVO> listGroupMembers(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.listGroupMembers(userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> inviteGroupMembers(Long conversationId, ChatGroupMemberOperateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.inviteGroupMembers(userId, conversationId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> appointGroupAdmin(Long conversationId, Long memberUserId) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.appointGroupAdmin(userId, conversationId, memberUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> removeGroupAdmin(Long conversationId, Long memberUserId) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.removeGroupAdmin(userId, conversationId, memberUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO transferGroupOwner(Long conversationId, ChatTransferGroupOwnerRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.transferGroupOwner(userId, conversationId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> muteGroupMember(Long conversationId, Long memberUserId, ChatMuteMemberRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.muteGroupMember(userId, conversationId, memberUserId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO updateGroupNotice(Long conversationId, ChatGroupNoticeUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return groupManageService.updateGroupNotice(userId, conversationId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGroupMember(Long conversationId, Long memberUserId) {
        Long userId = SecurityUtils.requireUserId();
        groupManageService.removeGroupMember(userId, conversationId, memberUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        groupManageService.leaveGroup(userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        groupManageService.dissolveGroup(userId, conversationId);
    }

    // ========== 大厅 & 频道加入/退出 ==========

    @Override
    public PageResult<ChatLobbyMessageVO> pageLobbyMessages(Long current, Long size, Long beforeMessageId) {
        return conversationQueryService.pageLobbyMessages(current, size, beforeMessageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO joinConversation(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        return channelJoinService.joinConversation(userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveConversation(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        channelJoinService.leaveConversation(userId, conversationId);
    }
}
