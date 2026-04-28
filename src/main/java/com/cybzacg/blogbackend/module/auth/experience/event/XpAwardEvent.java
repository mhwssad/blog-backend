package com.cybzacg.blogbackend.module.auth.experience.event;

import lombok.Getter;

/**
 * 经验入账事件。
 */
@Getter
public class XpAwardEvent {

    private final Long userId;
    private final String sourceType;
    private final String sourceBizId;
    private final String idempotentKey;

    public XpAwardEvent(Long userId, String sourceType, String sourceBizId, String idempotentKey) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.sourceBizId = sourceBizId;
        this.idempotentKey = idempotentKey;
    }
}
