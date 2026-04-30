package com.cybzacg.blogbackend.module.chat.governance.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.message.model.admin.*;

import java.util.List;

/**
 * 后台聊天查询服务接口。
 */
public interface ChatAdminQueryService {

    PageResult<ChatAdminConversationVO> pageConversations(ChatAdminConversationPageQuery query);

    ChatAdminConversationVO getConversation(Long conversationId);

    List<ChatMemberVO> listMembers(Long conversationId);

    PageResult<ChatAdminMessageVO> pageMessages(Long conversationId, ChatAdminMessagePageQuery query);

    ChatAdminMessageDetailVO getMessageDetail(Long conversationId, Long messageId);

    PageResult<ChatAdminMessageReceiptVO> pageMessageReceipts(Long conversationId, Long messageId, ChatAdminMessageReceiptPageQuery query);
}
