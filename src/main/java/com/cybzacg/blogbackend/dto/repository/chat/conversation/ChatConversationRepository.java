package com.cybzacg.blogbackend.module.chat.conversation.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatConversationListItem;

import java.util.List;

/**
 * 聊天会话 Repository。<p>
 * 封装会话实体的持久化操作，包括用户侧分页查询、管理后台分页查询以及单例会话查找。
 */
public interface ChatConversationRepository extends IService<ChatConversation> {

    /**
     * 统计用户参与的会话分页总数。
     */
    Long countConversationPage(Long userId, String keyword);

    /**
     * 查询用户参与的会话分页列表。
     */
    List<ChatConversationListItem> selectConversationPage(Long userId, String keyword, Long offset, Long size);

    /**
     * 查询单个会话的详情，包含当前用户视角的成员信息。
     */
    ChatConversationListItem selectConversationDetail(Long conversationId, Long userId);

    /**
     * 统计可搜索公开群聊数量。
     */
    Long countSearchableGroupPage(Long userId, String keyword, String categoryCode);

    /**
     * 分页查询可搜索公开群聊。
     */
    List<ChatConversationListItem> selectSearchableGroupPage(Long userId, String keyword, String categoryCode, Long offset, Long size);

    /**
     * 统计管理后台会话分页总数。
     */
    Long countAdminConversationPage(ChatAdminConversationPageQuery query);

    /**
     * 查询管理后台会话分页列表。
     */
    List<ChatAdminConversationListItem> selectAdminConversationPage(ChatAdminConversationPageQuery query, Long offset, Long size);

    /**
     * 查询管理后台单个会话详情。
     */
    ChatAdminConversationListItem selectAdminConversationDetail(Long conversationId);

    /**
     * 根据单聊唯一标识（singlePairKey）查找会话，用于判断单聊是否已存在。
     */
    ChatConversation findBySinglePairKey(String singlePairKey);

    /**
     * 查找全站广播会话，系统中最多只有一条。
     */
    ChatConversation findGlobalConversation();

    /**
     * 统计用户创建的正常普通群数量。
     */
    long countNormalGroupsByOwner(Long ownerId);
}
