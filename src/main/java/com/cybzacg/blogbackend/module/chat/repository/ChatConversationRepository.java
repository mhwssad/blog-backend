package com.cybzacg.blogbackend.module.chat.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatConversationListItem;

import java.util.List;

/**
 * 聊天会话 Repository。
 */
public interface ChatConversationRepository extends IService<ChatConversation> {
    Long countConversationPage(Long userId, String keyword);

    List<ChatConversationListItem> selectConversationPage(Long userId, String keyword, Long offset, Long size);

    ChatConversationListItem selectConversationDetail(Long conversationId, Long userId);

    Long countAdminConversationPage(ChatAdminConversationPageQuery query);

    List<ChatAdminConversationListItem> selectAdminConversationPage(ChatAdminConversationPageQuery query, Long offset, Long size);

    ChatAdminConversationListItem selectAdminConversationDetail(Long conversationId);

    ChatConversation findBySinglePairKey(String singlePairKey);

    ChatConversation findGlobalConversation();
}
