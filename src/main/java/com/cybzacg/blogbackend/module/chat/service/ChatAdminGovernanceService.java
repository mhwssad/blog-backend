package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberRoleUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberStatusUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;

import java.util.List;

/**
 * 后台聊天治理服务接口。
 */
public interface ChatAdminGovernanceService {

    List<ChatMemberVO> updateMemberRole(Long conversationId, Long memberUserId, ChatAdminMemberRoleUpdateRequest request);

    List<ChatMemberVO> updateMemberStatus(Long conversationId, Long memberUserId, ChatAdminMemberStatusUpdateRequest request);

    List<ChatMemberVO> updateMemberMute(Long conversationId, Long memberUserId, ChatAdminMemberMuteUpdateRequest request);

    void revokeMessage(Long conversationId, Long messageId);

    void updateConversationStatus(Long conversationId, Integer status);
}