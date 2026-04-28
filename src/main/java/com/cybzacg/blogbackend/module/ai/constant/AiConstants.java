package com.cybzacg.blogbackend.module.ai.constant;

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

    private AiConstants() {
    }
}
