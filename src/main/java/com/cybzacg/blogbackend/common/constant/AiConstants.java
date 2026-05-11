package com.cybzacg.blogbackend.common.constant;

/**
 * AI 模块常量。
 */
public final class AiConstants {
    public static final int DEFAULT_MAX_CONTEXT_MESSAGES = 50;
    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_MAX_TOKENS = 2048;
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;

    public static final String ROLE_TYPE_USER = "user";
    public static final String ROLE_TYPE_ASSISTANT = "assistant";
    public static final String ROLE_TYPE_SYSTEM = "system";

    public static final String SCENE_TYPE_GENERAL = "general";

    /** 单次用户输入最大字符数 */
    public static final int MAX_INPUT_LENGTH = 2000;

    /** 知识同步默认间隔（秒） */
    public static final int DEFAULT_KNOWLEDGE_SYNC_INTERVAL = 3600;
    /** 知识同步默认最大重试次数 */
    public static final int DEFAULT_KNOWLEDGE_MAX_RETRY = 3;

    /** 知识同步任务类型 */
    public static final String SYNC_TASK_TYPE_FULL = "full_sync";
    public static final String SYNC_TASK_TYPE_INCREMENTAL = "incremental_sync";
    public static final String SYNC_TASK_TYPE_SINGLE = "single_entry";

    /** 知识同步触发方式 */
    public static final String SYNC_TRIGGER_SYSTEM = "system";
    public static final String SYNC_TRIGGER_ADMIN = "admin";
    public static final String SYNC_TRIGGER_MANUAL = "manual";

    /** Agent 默认最大对话轮次 */
    public static final int DEFAULT_AGENT_MAX_TURNS = 1;

    /** 允许作为 AI 消息附件的文件类型 */
    public static final java.util.List<String> ALLOWED_ATTACHMENT_FILE_TYPES = java.util.List.of("image");
    /** 单条消息最大附件数 */
    public static final int MAX_ATTACHMENTS_PER_MESSAGE = 5;
    /** 单个附件最大文件大小 (10MB) */
    public static final long MAX_ATTACHMENT_FILE_SIZE = 10 * 1024 * 1024;

    private AiConstants() {
    }
}
