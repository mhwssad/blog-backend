package com.cybzacg.blogbackend.module.ai.listener;

import com.cybzacg.blogbackend.module.ai.event.ContentChangeEvent;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeEntryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内容变更 -> 知识库同步监听。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeSyncContentListener {

    private final AiKnowledgeEntryAdminService aiKnowledgeEntryAdminService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onContentChange(ContentChangeEvent event) {
        aiKnowledgeEntryAdminService.onContentChange(event);
    }
}
