package com.cybzacg.blogbackend.module.chat.shared.support;

import com.cybzacg.blogbackend.common.constant.ChatConstants;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversationMember;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 聊天成员排序与展示共享组件。
 */
@Component
public class ChatMemberHelper {

    public int memberRoleOrder(ChatConversationMember member) {
        if (Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER)) {
            return 0;
        }
        if (Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_ADMIN)) {
            return 1;
        }
        return 2;
    }
}
