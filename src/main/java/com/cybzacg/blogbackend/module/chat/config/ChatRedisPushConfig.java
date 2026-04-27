package com.cybzacg.blogbackend.module.chat.config;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.module.chat.service.impl.ChatPushRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 聊天 Redis 发布/订阅推送配置。<p>注册 RedisMessageListenerContainer 并订阅聊天推送 Topic，将消息分发至 ChatPushRedisSubscriber 处理。</p>
 */
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class ChatRedisPushConfig {
    private final ChatPushRedisSubscriber chatPushRedisSubscriber;

    /**
     * 创建聊天 Redis 消息监听容器，订阅聊天推送频道。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisMessageListenerContainer 实例
     */
    @Bean
    public RedisMessageListenerContainer chatRedisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(chatPushRedisSubscriber, new ChannelTopic(RedisConstants.CHAT_WS_PUSH_TOPIC));
        return container;
    }
}
