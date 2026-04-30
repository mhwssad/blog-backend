package com.cybzacg.blogbackend.module.chat.conversation.service;

import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatTopicChannelSaveRequest;

/**
 * 后台主题频道管理服务。
 */
public interface ChatTopicChannelAdminService {

    /**
     * 创建主题频道。
     */
    ChatAdminConversationVO createTopicChannel(ChatTopicChannelSaveRequest request);

    /**
     * 编辑主题频道配置。
     */
    ChatAdminConversationVO updateTopicChannel(Long conversationId, ChatTopicChannelSaveRequest request);
}
