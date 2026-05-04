package com.cybzacg.blogbackend.enums.forum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 论坛内容可见范围。
 */
@Getter
@AllArgsConstructor
public enum ForumVisibilityScopeEnum {
    PUBLIC(0, "公开"),
    LOGIN_ONLY(1, "登录可见");

    private final Integer value;
    private final String description;
}
