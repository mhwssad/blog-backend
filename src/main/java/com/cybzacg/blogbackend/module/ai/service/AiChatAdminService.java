package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionAdminVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionPageQuery;

/**
 * AI 会话后台管理服务接口。
 *
 * <p>提供管理员按条件查询用户 AI 会话的能力。
 */
public interface AiChatAdminService {

    /**
     * 后台分页查询用户 AI 会话。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<AiSessionAdminVO> pageSessions(AiSessionPageQuery query);

    /**
     * 后台查询会话详情。
     *
     * @param sessionId 会话ID
     * @return 会话详情
     */
    AiSessionAdminVO getSessionDetail(Long sessionId);
}
