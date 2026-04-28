package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;

/**
 * 主题频道公开查询服务。
 */
public interface ChatTopicChannelPublicService {
    PageResult<ChatConversationVO> pageChannels(Long current, Long size, String categoryCode);
    ChatConversationVO getChannel(Long conversationId);
}
