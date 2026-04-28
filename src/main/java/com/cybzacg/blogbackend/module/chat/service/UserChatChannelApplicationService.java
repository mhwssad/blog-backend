package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.user.ChatChannelApplicationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatChannelApplicationSubmitRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatChannelApplicationVO;

/**
 * 用户侧频道创建申请服务。
 */
public interface UserChatChannelApplicationService {

    /**
     * 提交频道创建申请。
     */
    ChatChannelApplicationVO submitApplication(ChatChannelApplicationSubmitRequest request);

    /**
     * 查询当前用户最近一次频道创建申请。
     */
    ChatChannelApplicationVO getLatestApplication();

    /**
     * 分页查询当前用户自己的频道创建申请。
     */
    PageResult<ChatChannelApplicationVO> pageMyApplications(ChatChannelApplicationPageQuery query);
}
