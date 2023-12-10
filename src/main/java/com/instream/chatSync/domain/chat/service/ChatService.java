package com.instream.chatSync.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ChatService {
    private final ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate;
    private final MessageStorageService messageStorageService;
    private final ConcurrentHashMap<UUID, Disposable> subscribeSessionList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Disposable> endSessionList = new ConcurrentHashMap<>();


    @Autowired
    public ChatService(ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate,
                       MessageStorageService messageStorageService) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.messageStorageService = messageStorageService;
    }

    public Mono<Void> postConnection(UUID sessionId) {
        subscribeSessionList.computeIfAbsent(sessionId, key -> {
            ChannelTopic chatTopic = new ChannelTopic(sessionId.toString());
            ChannelTopic closeTopic = new ChannelTopic(sessionId + "_END");
            Disposable chatDisposable = getRedisChatMessageTopicSubscribe(sessionId, chatTopic).subscribe();
            Disposable closeDisposable = getRedisCloseMessageTopicSubscribe(closeTopic).subscribe();

            endSessionList.put(sessionId, closeDisposable);

            return chatDisposable;
        });
        return Mono.empty();
    }

    public Flux<ServerSentEvent<List<String>>> streamMessages(UUID sessionId) {
        return messageStorageService.streamMessages(sessionId);
    }

    private Flux<? extends ReactiveSubscription.Message<String, String>> getRedisChatMessageTopicSubscribe(UUID sessionId, ChannelTopic topic) {
        return reactiveStringRedisTemplate.listenTo(topic)
                .doOnNext(message -> messageStorageService.addMessage(sessionId, message.getMessage()))
                .doOnSubscribe(subscription -> messageStorageService.addPublishFlux(sessionId));
    }

    private Flux<? extends ReactiveSubscription.Message<String, String>> getRedisCloseMessageTopicSubscribe(ChannelTopic topic) {
        return reactiveStringRedisTemplate.listenTo(topic)
                .doOnNext(message -> this.closeSession(message.getChannel()));
    }

    private void closeSession(String endChannel) {
        UUID endSessionId = UUID.fromString(endChannel.split("_END")[0]);
        subscribeSessionList.remove(endSessionId).dispose();
        endSessionList.remove(endSessionId).dispose();
    }
}

