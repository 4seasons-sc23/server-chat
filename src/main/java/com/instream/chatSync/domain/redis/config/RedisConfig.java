//package com.instream.chatSync.domain.redis.config;
//
//import com.instream.chatSync.domain.chat.service.MessageStorageService;
//import com.instream.chatSync.domain.redis.RedisMessageSubscriber;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
////import org.springframework.data.redis.listener.ChannelTopic;
//import org.springframework.data.redis.core.ReactiveRedisTemplate;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.listener.RedisMessageListenerContainer;
//import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
//
//@Configuration
//public class RedisConfig {
//
//    private final MessageStorageService messageStorageService;
//    @Value("${spring.data.redis.host}")
//    private String REDIS_HOST;
//
//    @Value("${spring.data.redis.port}")
//    private int REDIS_PORT;
//
//    public RedisConfig(MessageStorageService messageStorageService) {
//        this.messageStorageService = messageStorageService;
//    }
//
//
//    @Bean
//    public ReactiveRedisConnectionFactory redisConnectionFactory() {
//        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT);
//        return new LettuceConnectionFactory(config);
//    }
//
//    @Bean
//    MessageListenerAdapter messageStringListener() {
//        return new MessageListenerAdapter(new RedisMessageSubscriber(messageStorageService));
//    }
//}