package com.cybzacg.blogbackend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SysAuditOperationType {
    BAN_USER("BAN_USER", "封禁用户"),
    UNBAN_USER("UNBAN_USER", "解封用户"),
    ASSIGN_ADMIN_ROLE("ASSIGN_ADMIN_ROLE", "分配管理员角色"),
    TAKEOVER_ACCOUNT("TAKEOVER_ACCOUNT", "账号接管"),
    ADJUST_LEVEL("ADJUST_LEVEL", "调整等级"),
    ADJUST_EXPERIENCE("ADJUST_EXPERIENCE", "调整经验"),
    FINALIZE_REPORT("FINALIZE_REPORT", "举报终结"),
    MODIFY_AI_CONFIG("MODIFY_AI_CONFIG", "修改AI配置"),
    MODIFY_ADMIN_PERM("MODIFY_ADMIN_PERM", "修改管理员权限"),
    TOGGLE_ARTICLE_PIN("TOGGLE_ARTICLE_PIN", "切换文章置顶"),
    TOGGLE_ARTICLE_RECOMMEND("TOGGLE_ARTICLE_RECOMMEND", "切换文章推荐");

    private final String code;
    private final String description;

    public static String getDescriptionByCode(String code) {
        for (SysAuditOperationType type : values()) {
            if (type.code.equals(code)) {
                return type.description;
            }
        }
        return code;
    }
}
