//package com.instream.chatSync.domain.redis.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.util.concurrent.ConcurrentHashMap;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
//import org.springframework.data.redis.core.ReactiveRedisTemplate;
//import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ReactiveRedisTemplateFactory {
//    private final ReactiveRedisConnectionFactory factory;
//
//    private final ConcurrentHashMap<Class<?>, ReactiveRedisTemplate<String, ?>> templateCache = new ConcurrentHashMap<>();
//
//    private final ObjectMapper objectMapper;
//
//    @Autowired
//    public ReactiveRedisTemplateFactory(ReactiveRedisConnectionFactory factory, ObjectMapper objectMapper) {
//        this.factory = factory;
//        this.objectMapper = objectMapper;
//    }
//
//    public <T> ReactiveRedisTemplate<String, T> getTemplate(Class<T> clazz) {
//        return (ReactiveRedisTemplate<String, T>) templateCache.computeIfAbsent(clazz, this::createReactiveRedisTemplate);
//    }
//
//    private <T> ReactiveRedisTemplate<String, T> createReactiveRedisTemplate(Class<T> clazz) {
//        Jackson2JsonRedisSerializer<T> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, clazz);
//        RedisSerializationContext.RedisSerializationContextBuilder<String, T> builder =
//                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
//
//        RedisSerializationContext<String, T> context = builder.value(serializer).build();
//
//        return new ReactiveRedisTemplate<>(factory, context);
//    }
//}
