package com.cybzacg.blogbackend.module.chat.member.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationAdminVO;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationReviewRequest;

/**
 * 后台群入群申请管理服务。
 */
public interface ChatGroupJoinApplicationAdminService {

    /**
     * 分页查询群入群申请。
     */
    PageResult<ChatGroupJoinApplicationAdminVO> pageApplications(ChatGroupJoinApplicationAdminPageQuery query);

    /**
     * 查询群入群申请详情。
     */
    ChatGroupJoinApplicationAdminVO getApplication(Long id);

    /**
     * 审核群入群申请。
     */
    void reviewApplication(Long id, ChatGroupJoinApplicationReviewRequest request);
}
