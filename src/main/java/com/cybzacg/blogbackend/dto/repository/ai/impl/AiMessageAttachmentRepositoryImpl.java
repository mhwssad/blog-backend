package com.cybzacg.blogbackend.dto.repository.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiMessageAttachment;
import com.cybzacg.blogbackend.dto.mapper.ai.AiMessageAttachmentMapper;
import com.cybzacg.blogbackend.dto.repository.ai.AiMessageAttachmentRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class AiMessageAttachmentRepositoryImpl
        extends ServiceImpl<AiMessageAttachmentMapper, AiMessageAttachment>
        implements AiMessageAttachmentRepository {

    @Override
    public List<AiMessageAttachment> listByMessageId(Long messageId) {
        return list(new LambdaQueryWrapper<AiMessageAttachment>()
                .eq(AiMessageAttachment::getMessageId, messageId));
    }

    @Override
    public List<AiMessageAttachment> listByMessageIds(Collection<Long> messageIds) {
        return list(new LambdaQueryWrapper<AiMessageAttachment>()
                .in(AiMessageAttachment::getMessageId, messageIds));
    }
}
