package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatChannelApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatChannelApplicationAdminVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatChannelApplicationReviewRequest;

/**
 * 后台频道创建申请管理服务。
 */
public interface ChatChannelApplicationAdminService {

    /**
     * 分页查询频道创建申请。
     */
    PageResult<ChatChannelApplicationAdminVO> pageApplications(ChatChannelApplicationAdminPageQuery query);

    /**
     * 查询频道创建申请详情。
     */
    ChatChannelApplicationAdminVO getApplication(Long id);

    /**
     * 审核频道创建申请。
     */
    void reviewApplication(Long id, ChatChannelApplicationReviewRequest request);
}
