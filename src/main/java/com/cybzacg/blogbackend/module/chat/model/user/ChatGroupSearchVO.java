package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊搜索结果。
 */
@Data
@Schema(description = "群聊搜索结果")
public class ChatGroupSearchVO {
    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    private String description;
    private String notice;
    private String visibilityScope;
    private String joinRule;
    private Integer speakLevelLimit;
    private Integer memberLimit;
    private String channelCategoryCode;
    private Long memberCount;
    private Boolean joined;
    private String selfRole;
    private LocalDateTime createdAt;
}
