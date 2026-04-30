package com.cybzacg.blogbackend.module.chat.member.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupSearchQuery;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupSearchVO;

/**
 * 用户侧群聊发现服务。
 */
public interface UserChatGroupDiscoveryService {

    /**
     * 分页搜索公开群聊。
     */
    PageResult<ChatGroupSearchVO> searchGroups(ChatGroupSearchQuery query);
}
