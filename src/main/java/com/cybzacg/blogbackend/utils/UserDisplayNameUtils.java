package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.domain.SysUser;

/**
 * 用户显示名工具类。
 *
 * <p>统一收敛用户显示名的解析逻辑：优先取昵称，其次取用户名，均无时返回 "用户" + id。
 */
public final class UserDisplayNameUtils {

    private UserDisplayNameUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 解析用户显示名。
     *
     * @param user            用户对象，nullable
     * @param fallbackUserId  user 为 null 时的备用用户 ID，nullable
     * @return 用户显示名字符串
     */
    public static String resolveDisplayName(SysUser user, Long fallbackUserId) {
        if (user == null) {
            return fallbackUserId == null ? null : "用户" + fallbackUserId;
        }
        if (StrUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StrUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return user.getId() == null ? null : "用户" + user.getId();
    }
}
