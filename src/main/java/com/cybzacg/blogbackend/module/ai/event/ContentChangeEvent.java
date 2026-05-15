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

    /**
     * 构造内容变更事件。
     *
     * @param sourceType 来源类型（如 article、forum_post 等）
     * @param sourceId   来源对象ID
     * @param action     变更操作类型
     * @param authorId   内容作者ID
     */
    public ContentChangeEvent(String sourceType, Long sourceId,
                              ContentChangeAction action, Long authorId) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.action = action;
        this.authorId = authorId;
    }
}
