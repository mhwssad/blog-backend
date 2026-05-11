package com.cybzacg.blogbackend.dto.repository.chat.governance;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.chat.ChatUserMuteRecord;

import java.util.List;

/**
 * ChatUserMuteRecord Repository。
 */
public interface ChatUserMuteRecordRepository extends IService<ChatUserMuteRecord> {

    /**
     * 查询用户在指定范围下的生效禁言记录。
     */
    List<ChatUserMuteRecord> findActiveByUserIdAndScope(Long userId, String scope);

    /**
     * 查询用户在指定会话下的生效禁言记录。
     */
    List<ChatUserMuteRecord> findActiveByUserIdAndConversationId(Long userId, Long conversationId);

    /**
     * 查询用户的所有生效禁言记录（用于全站+会话范围检查）。
     */
    List<ChatUserMuteRecord> findAllActiveByUserId(Long userId);

    /**
     * 后台分页查询禁言记录。
     */
    Page<ChatUserMuteRecord> pageByAdminConditions(Long userId, String scope, Integer status, Page<ChatUserMuteRecord> page);
}
