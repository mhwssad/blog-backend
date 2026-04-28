package com.cybzacg.blogbackend.enums.chat;

import lombok.Getter;

/**
 * 聊天会话加入规则枚举。
 */
@Getter
public enum ChatConversationJoinRuleEnum {
    FREE("free", "自由加入"),
    APPROVAL("approval", "审批加入"),
    INVITE_ONLY("invite_only", "仅邀请加入");

    private final String code;
    private final String label;

    ChatConversationJoinRuleEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ChatConversationJoinRuleEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChatConversationJoinRuleEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(String code) {
        return fromCode(code) != null;
    }
}
