package com.cybzacg.blogbackend.enums.forum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 论坛帖子状态。
 */
@Getter
@AllArgsConstructor
public enum ForumPostStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    REVIEWING(2, "审核中"),
    REJECTED(3, "已拒绝"),
    DELETED(4, "已删除");

    private final Integer value;
    private final String description;
}
