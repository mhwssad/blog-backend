package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容变更动作枚举。
 */
@Getter
@AllArgsConstructor
public enum ContentChangeAction {

    PUBLISH("publish", "发布"),
    UPDATE("update", "更新"),
    HIDE("hide", "隐藏"),
    RESTORE("restore", "恢复"),
    DELETE("delete", "删除");

    private final String code;
    private final String description;
}
