package com.cybzacg.blogbackend.module.chat.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ChatGroupJoinApplication;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupJoinApplicationPageQuery;

/**
 * 群聊入群申请 Repository。
 */
public interface ChatGroupJoinApplicationRepository extends IService<ChatGroupJoinApplication> {

    /**
     * 查询指定用户在指定会话下的最新入群申请。
     */
    ChatGroupJoinApplication findLatestByConversationAndApplicant(Long conversationId, Long applicantUserId);

    /**
     * 分页查询当前用户自己的入群申请。
     */
    Page<ChatGroupJoinApplication> pageByApplicantUserId(Long applicantUserId, ChatGroupJoinApplicationPageQuery query);

    /**
     * 分页查询指定会话下的入群申请。
     */
    Page<ChatGroupJoinApplication> pageByConversationId(Long conversationId, ChatGroupJoinApplicationPageQuery query);
}
