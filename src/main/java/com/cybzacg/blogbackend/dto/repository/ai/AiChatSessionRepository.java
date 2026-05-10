package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.module.ai.model.admin.AiSessionPageQuery;

import java.util.List;

/**
 * AiChatSession Repository。
 */
public interface AiChatSessionRepository extends IService<AiChatSession> {

    /**
     * 查询用户自己的 AI 会话。
     */
    AiChatSession findByIdAndUserId(Long sessionId, Long userId);

    /**
     * 按用户、状态读取最近会话。
     */
    List<AiChatSession> listByUserIdAndStatusOrderByUpdatedAt(Long userId, Integer status, int limit);

    /**
     * 按用户分页查询会话（按更新时间倒序）。
     *
     * @param userId  用户ID
     * @param status  会话状态（可为 null 表示不限）
     * @param current 页码
     * @param size    每页条数
     * @return 分页结果
     */
    Page<AiChatSession> pageByUserIdAndStatus(Long userId, Integer status, long current, long size);

    /**
     * 后台按条件分页查询会话。
     */
    Page<AiChatSession> pageByAdminConditions(AiSessionPageQuery query);
}
