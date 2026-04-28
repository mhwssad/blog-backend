package com.cybzacg.blogbackend.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 经验体系错误码。
 */
@Getter
@AllArgsConstructor
public enum ExperienceResultCode implements ResultCode {

    XP_DAILY_CAP_REACHED(72001, "今日经验已达上限"),
    XP_SOURCE_DISABLED(72002, "该经验来源已关闭"),
    XP_DUPLICATE_AWARD(72003, "重复经验发放"),
    XP_LEVEL_INSUFFICIENT(72004, "等级不足"),
    XP_LEVEL_CONFIG_NOT_FOUND(72005, "等级配置不存在");

    private final Integer code;
    private final String message;
}
