package com.Dolmeng_E.drive.common.config;

import com.Dolmeng_E.drive.common.service.RedisSubscriber;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    // 우리가 만든 Redis Subscriber를 MessageListener로 사용하기 위한 어댑터
    @Bean
    public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
        // onMessage 메서드가 메시지를 처리하도록 설정
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    // Redis 메시지 리스너 컨테이너
    @Bean
    public RedisMessageListenerContainer redisMessageListener(@Qualifier("document")RedisConnectionFactory connectionFactory,
                                                              MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // "document-updates"라는 채널을 구독
        container.addMessageListener(listenerAdapter, new ChannelTopic("document-updates"));
        return container;
    }

    @Bean
    @Qualifier("document")
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("document")RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("document")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(1);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("userInventory")
    public RedisTemplate<String, String> userRedisTemplate(@Qualifier("userInventory")RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("userInventory")
    public RedisConnectionFactory userRedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(2);
        return new LettuceConnectionFactory(configuration);
    }
}