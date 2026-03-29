package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberRoleUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberStatusUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageDetailVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptPageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import java.util.List;

/**
 * 聊天后台管理服务接口。
 */
public interface ChatAdminService {
    PageResult<ChatAdminConversationVO> pageConversations(ChatAdminConversationPageQuery query);

    ChatAdminConversationVO getConversation(Long conversationId);

    List<ChatMemberVO> listMembers(Long conversationId);

    PageResult<ChatAdminMessageVO> pageMessages(Long conversationId, ChatAdminMessagePageQuery query);

    ChatAdminMessageDetailVO getMessageDetail(Long conversationId, Long messageId);

    PageResult<ChatAdminMessageReceiptVO> pageMessageReceipts(Long conversationId, Long messageId, ChatAdminMessageReceiptPageQuery query);

    List<ChatMemberVO> updateMemberRole(Long conversationId, Long memberUserId, ChatAdminMemberRoleUpdateRequest request);

    List<ChatMemberVO> updateMemberStatus(Long conversationId, Long memberUserId, ChatAdminMemberStatusUpdateRequest request);

    List<ChatMemberVO> updateMemberMute(Long conversationId, Long memberUserId, ChatAdminMemberMuteUpdateRequest request);

    void revokeMessage(Long conversationId, Long messageId);

    void updateConversationStatus(Long conversationId, Integer status);
}
