package com.cybzacg.blogbackend.module.chat.config;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.module.chat.service.impl.ChatPushRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 聊天 Redis 推送配置。
 */
@Configuration
@RequiredArgsConstructor
public class ChatRedisPushConfig {
    private final ChatPushRedisSubscriber chatPushRedisSubscriber;

    @Bean
    public RedisMessageListenerContainer chatRedisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(chatPushRedisSubscriber, new ChannelTopic(RedisConstants.CHAT_WS_PUSH_TOPIC));
        return container;
    }
}
