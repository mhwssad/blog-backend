package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.admin.*;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.service.ChatAdminGovernanceService;
import com.cybzacg.blogbackend.module.chat.service.ChatAdminQueryService;
import com.cybzacg.blogbackend.module.chat.service.ChatAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 后台聊天管理服务门面。
 *
 * <p>委托调用到两个子 Service：
 * <ul>
 *     <li>{@link ChatAdminQueryService} — 后台查询（会话/成员/消息/回执）</li>
 *     <li>{@link ChatAdminGovernanceService} — 后台治理（角色/状态/禁言/撤回/状态）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ChatAdminServiceImpl implements ChatAdminService {

    private final ChatAdminQueryService chatAdminQueryService;
    private final ChatAdminGovernanceService chatAdminGovernanceService;

    @Override
    public PageResult<ChatAdminConversationVO> pageConversations(ChatAdminConversationPageQuery query) {
        return chatAdminQueryService.pageConversations(query);
    }

    @Override
    public ChatAdminConversationVO getConversation(Long conversationId) {
        return chatAdminQueryService.getConversation(conversationId);
    }

    @Override
    public List<ChatMemberVO> listMembers(Long conversationId) {
        return chatAdminQueryService.listMembers(conversationId);
    }

    @Override
    public PageResult<ChatAdminMessageVO> pageMessages(Long conversationId, ChatAdminMessagePageQuery query) {
        return chatAdminQueryService.pageMessages(conversationId, query);
    }

    @Override
    public ChatAdminMessageDetailVO getMessageDetail(Long conversationId, Long messageId) {
        return chatAdminQueryService.getMessageDetail(conversationId, messageId);
    }

    @Override
    public PageResult<ChatAdminMessageReceiptVO> pageMessageReceipts(Long conversationId, Long messageId, ChatAdminMessageReceiptPageQuery query) {
        return chatAdminQueryService.pageMessageReceipts(conversationId, messageId, query);
    }

    @Override
    public List<ChatMemberVO> updateMemberRole(Long conversationId, Long memberUserId, ChatAdminMemberRoleUpdateRequest request) {
        return chatAdminGovernanceService.updateMemberRole(conversationId, memberUserId, request);
    }

    @Override
    public List<ChatMemberVO> updateMemberStatus(Long conversationId, Long memberUserId, ChatAdminMemberStatusUpdateRequest request) {
        return chatAdminGovernanceService.updateMemberStatus(conversationId, memberUserId, request);
    }

    @Override
    public List<ChatMemberVO> updateMemberMute(Long conversationId, Long memberUserId, ChatAdminMemberMuteUpdateRequest request) {
        return chatAdminGovernanceService.updateMemberMute(conversationId, memberUserId, request);
    }

    @Override
    public void revokeMessage(Long conversationId, Long messageId) {
        chatAdminGovernanceService.revokeMessage(conversationId, messageId);
    }

    @Override
    public void updateConversationStatus(Long conversationId, Integer status) {
        chatAdminGovernanceService.updateConversationStatus(conversationId, status);
    }
}