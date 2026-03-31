package com.cybzacg.blogbackend.module.chat.constant;

/**
 * 聊天模块常量。
 */
public final class ChatConstants {
    public static final String CONVERSATION_TYPE_SINGLE = "single";
    public static final String CONVERSATION_TYPE_GROUP = "group";
    public static final String CONVERSATION_TYPE_GLOBAL = "global";

    public static final String MEMBER_ROLE_OWNER = "owner";
    public static final String MEMBER_ROLE_ADMIN = "admin";
    public static final String MEMBER_ROLE_MEMBER = "member";

    public static final String JOIN_SOURCE_MANUAL = "manual";
    public static final String JOIN_SOURCE_SYSTEM = "system";

    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_FILE = "file";
    public static final String MESSAGE_TYPE_IMAGE = "image";
    public static final String MESSAGE_TYPE_VOICE = "voice";
    public static final String ATTACHMENT_TRANSCODE_STATUS_SOURCE = "source";
    public static final String ATTACHMENT_TRANSCODE_STATUS_PENDING = "pending";
    public static final String ATTACHMENT_TRANSCODE_STATUS_READY = "ready";
    public static final String ATTACHMENT_TRANSCODE_STATUS_FAILED = "failed";
    public static final String REPLY_MESSAGE_UNAVAILABLE_PLACEHOLDER = "引用消息已不可见";
    public static final String REPLY_STATE_NORMAL = "normal";
    public static final String REPLY_STATE_REVOKED = "revoked";
    public static final String REPLY_STATE_UNAVAILABLE = "unavailable";

    public static final int CONVERSATION_STATUS_DISABLED = 0;
    public static final int CONVERSATION_STATUS_NORMAL = 1;
    public static final int CONVERSATION_STATUS_DISSOLVED = 2;

    public static final int MEMBER_STATUS_LEFT = 0;
    public static final int MEMBER_STATUS_NORMAL = 1;
    public static final int MEMBER_STATUS_REMOVED = 2;
    public static final int MEMBER_STATUS_DISABLED = 3;

    public static final int SEND_STATUS_SENT = 1;
    public static final int REVOKE_STATUS_NORMAL = 0;
    public static final int REVOKE_STATUS_REVOKED = 1;

    public static final int DELIVERY_STATUS_PENDING = 0;
    public static final int DELIVERY_STATUS_DELIVERED = 1;
    public static final int DELIVERY_STATUS_READ = 2;

    public static final int VISIBLE_STATUS_HIDDEN = 0;
    public static final int VISIBLE_STATUS_VISIBLE = 1;

    public static final String GLOBAL_CONVERSATION_NAME = "全站聊天室";
    public static final String FILE_MESSAGE_REFERENCE_TYPE = "chat_message";
    public static final String FILE_MESSAGE_CATEGORY = "chat_attachment";
    public static final String MESSAGE_REVOKED_PLACEHOLDER = "消息已撤回";
    public static final int ATTACHMENT_TASK_STATUS_PENDING = 0;
    public static final int ATTACHMENT_TASK_STATUS_PROCESSING = 1;
    public static final int ATTACHMENT_TASK_STATUS_SUCCESS = 2;
    public static final int ATTACHMENT_TASK_STATUS_FAILED = 3;

    private ChatConstants() {
    }
}
