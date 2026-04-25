package com.cybzacg.blogbackend.module.chat.model.internal;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * 聊天分布式推送事件包。
 *
 * <p>用于把当前节点生成的 WebSocket 推送广播到 Redis，供其他节点转发到各自本地会话。
 */
@Data
public class ChatPushEventEnvelope {
    private String originNodeId;
    private String type;
    private List<Long> userIds;
    private JsonNode payload;
}
