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
    MODIFY_AI_TOOL("MODIFY_AI_TOOL", "修改AI工具配置"),
    MODIFY_AI_MCP_SERVER("MODIFY_AI_MCP_SERVER", "修改MCP服务配置"),
    MODIFY_AI_TOOL_AUTHORIZATION("MODIFY_AI_TOOL_AUTHORIZATION", "修改AI工具授权"),
    MODIFY_ADMIN_PERM("MODIFY_ADMIN_PERM", "修改管理员权限"),
    TOGGLE_ARTICLE_PIN("TOGGLE_ARTICLE_PIN", "切换文章置顶"),
    TOGGLE_ARTICLE_RECOMMEND("TOGGLE_ARTICLE_RECOMMEND", "切换文章推荐"),
    OVERRIDE_CLAIM_REPORT("OVERRIDE_CLAIM_REPORT", "超管接管举报"),
    TOGGLE_FORUM_POST_PIN("TOGGLE_FORUM_POST_PIN", "切换论坛帖子置顶"),
    TOGGLE_FORUM_POST_ESSENCE("TOGGLE_FORUM_POST_ESSENCE", "切换论坛帖子精华"),
    HIDE_FORUM_POST("HIDE_FORUM_POST", "隐藏论坛帖子"),
    RESTORE_FORUM_POST("RESTORE_FORUM_POST", "恢复论坛帖子"),
    DELETE_FORUM_POST("DELETE_FORUM_POST", "删除论坛帖子"),
    HIDE_FORUM_REPLY("HIDE_FORUM_REPLY", "隐藏论坛回复"),
    RESTORE_FORUM_REPLY("RESTORE_FORUM_REPLY", "恢复论坛回复"),
    DELETE_FORUM_REPLY("DELETE_FORUM_REPLY", "删除论坛回复");

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
