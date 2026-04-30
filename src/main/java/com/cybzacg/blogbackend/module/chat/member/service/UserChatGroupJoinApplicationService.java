package com.cybzacg.blogbackend.module.chat.member.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinApplicationPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinApplicationVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinApplyRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinReviewRequest;

/**
 * 用户侧入群申请服务。
 */
public interface UserChatGroupJoinApplicationService {

    /**
     * 提交入群申请。
     */
    ChatGroupJoinApplicationVO submitApplication(Long conversationId, ChatGroupJoinApplyRequest request);

    /**
     * 分页查询当前用户自己的入群申请。
     */
    PageResult<ChatGroupJoinApplicationVO> pageMyApplications(ChatGroupJoinApplicationPageQuery query);

    /**
     * 群主或管理员分页查询指定群的入群申请。
     */
    PageResult<ChatGroupJoinApplicationVO> pageGroupApplications(Long conversationId, ChatGroupJoinApplicationPageQuery query);

    /**
     * 群主或管理员审核指定入群申请。
     */
    void reviewApplication(Long conversationId, Long applicationId, ChatGroupJoinReviewRequest request);
}
