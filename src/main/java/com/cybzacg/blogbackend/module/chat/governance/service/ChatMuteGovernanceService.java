package com.cybzacg.blogbackend.module.chat.governance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMuteCreateRequest;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMuteRecordVO;

/**
 * 统一禁言治理服务。
 */
public interface ChatMuteGovernanceService {

    /**
     * 创建禁言记录。
     */
    ChatMuteRecordVO createMute(ChatMuteCreateRequest request, Long operatorId);

    /**
     * 解除禁言。
     */
    void releaseMute(Long recordId, Long operatorId);

    /**
     * 分页查询禁言记录。
     */
    Page<ChatMuteRecordVO> pageMutes(Long userId, String scope, Integer status, Long current, Long size);

    /**
     * 判断用户在指定会话中是否被禁言（用于发送拦截）。
     * scope 由调用方根据会话类型确定。
     */
    boolean isUserMuted(Long userId, Long conversationId, String scope);

    /**
     * 通过举报创建禁言（来源为 report）。
     */
    ChatMuteRecordVO createMuteFromReport(Long userId, String scope, Long conversationId,
                                          String reason, Long reportId, Long operatorId,
                                          java.time.LocalDateTime muteUntil);
}
