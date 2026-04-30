package com.cybzacg.blogbackend.module.chat.message.service;

/**
 * 聊天消息治理服务。
 *
 * <p>负责聊天域自己的发送频控、敏感词校验等入口治理规则。
 */
public interface ChatMessageGovernanceService {
    void validateTextMessage(Long userId, String content);

    void validateAttachmentMessage(Long userId);
}
