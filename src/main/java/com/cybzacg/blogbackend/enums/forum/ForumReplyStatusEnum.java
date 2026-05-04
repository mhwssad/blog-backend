package com.cybzacg.blogbackend.enums.forum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 论坛回复状态。
 */
@Getter
@AllArgsConstructor
public enum ForumReplyStatusEnum {
    NORMAL(1, "正常"),
    HIDDEN(2, "隐藏"),
    DELETED(3, "删除"),
    REVIEWING(4, "审核中");

    private final Integer value;
    private final String description;
}
