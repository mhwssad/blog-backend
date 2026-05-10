package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.ai.AiMessageAttachment;

import java.util.Collection;
import java.util.List;

/**
 * AI 消息附件 Repository。
 */
public interface AiMessageAttachmentRepository extends IService<AiMessageAttachment> {

    /**
     * 按消息 ID 查询附件列表。
     */
    List<AiMessageAttachment> listByMessageId(Long messageId);

    /**
     * 按消息 ID 列表批量查询附件。
     */
    List<AiMessageAttachment> listByMessageIds(Collection<Long> messageIds);
}
