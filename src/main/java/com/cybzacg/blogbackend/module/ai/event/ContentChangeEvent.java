package com.cybzacg.blogbackend.module.ai.event;

import com.cybzacg.blogbackend.enums.ai.ContentChangeAction;
import lombok.Getter;

/**
 * 内容变更事件，用于通知知识库同步。
 */
@Getter
public class ContentChangeEvent {

    private final String sourceType;
    private final Long sourceId;
    private final ContentChangeAction action;
    private final Long authorId;

    public ContentChangeEvent(String sourceType, Long sourceId,
                              ContentChangeAction action, Long authorId) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.action = action;
        this.authorId = authorId;
    }
}
