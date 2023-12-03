package com.instream.chatSync.domain.chat.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ChatService {
    private final ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate;
    private final MessageStorageService messageStorageService;
    private final ConcurrentHashMap<String, Disposable> subscribeSessionList = new ConcurrentHashMap<>();

    @Autowired
    public ChatService(ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate,
        MessageStorageService messageStorageService) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.messageStorageService = messageStorageService;
    }

    public Mono<Void> postConnection(String sessionId) {
        return Mono.fromRunnable(() -> {
            if(!subscribeSessionList.containsKey(sessionId)) {
                log.info("Create redis connection {}", sessionId);
                ChannelTopic topic = new ChannelTopic(sessionId);
                Disposable disposable = reactiveStringRedisTemplate.listenTo(topic)
                    .doOnNext(message -> messageStorageService.addMessage(sessionId, message.getMessage()))
                    .subscribe();
                messageStorageService.addPublishFlux(sessionId);
                subscribeSessionList.put(sessionId, disposable);
            }
        }).then();
    }

    public Flux<ServerSentEvent<List<String>>> streamMessages(String sessionId) {
        return messageStorageService.streamMessages(sessionId);
    }
}