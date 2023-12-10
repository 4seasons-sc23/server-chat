package com.instream.chatSync.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
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
    private final ConcurrentHashMap<UUID, Subscription> subscribeSessionList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Subscription> endSessionList = new ConcurrentHashMap<>();


    @Autowired
    public ChatService(ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate,
                       MessageStorageService messageStorageService) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.messageStorageService = messageStorageService;
    }

    /**
     * Chat과 관련된 Redis Topic을 구독합니다.
     * <p>
     * ChatMessageTopic이 먼저 구독되어야 합니다. 그래야 Close event가 발생했을 때 올바르게 모든 subscribe를 종료시킬 수 있습니다.
     * CloseTopic을 먼저 구독하면 ChatMessageTopic에 대한 subscribe를 종료시킬 수 없는 경우의 수가 생깁니다.
     */
    public Mono<Void> postConnection(UUID sessionId) {
        getRedisChatMessageTopicSubscribe(sessionId);
        getRedisCloseMessageTopicSubscribe(sessionId);

        return Mono.empty();
    }

    public Flux<ServerSentEvent<List<String>>> streamMessages(UUID sessionId) {
        return messageStorageService.streamMessages(sessionId);
    }

    /**
     * ChatMessageTopic을 구독합니다.
     *
     * Race condition을 방지하기 위해서 동기화 블록을 사용합니다.
     */
    private void getRedisChatMessageTopicSubscribe(UUID sessionId) {
        synchronized (this) {
            if (subscribeSessionList.containsKey(sessionId)) {
                return;
            }

            ChannelTopic topic = new ChannelTopic(sessionId.toString());
            reactiveStringRedisTemplate.listenTo(topic)
                    .doOnNext(message -> messageStorageService.addMessage(sessionId, message.getMessage()))
                    .doOnSubscribe(subscription -> subscribeSessionList.computeIfAbsent(sessionId, key -> {
                        messageStorageService.addPublishFlux(sessionId);
                        return subscription;
                    }))
                    .subscribe();
        }
    }

    /**
     * CloseTopic을 구독합니다.
     *
     * Race condition을 방지하기 위해서 동기화 블록을 사용합니다.
     */
    private void getRedisCloseMessageTopicSubscribe(UUID sessionId) {
        synchronized (this) {
            if (endSessionList.containsKey(sessionId)) {
                return;
            }
            ChannelTopic topic = new ChannelTopic(sessionId + "_END");
            reactiveStringRedisTemplate.listenTo(topic)
                    .doOnNext(message -> closeSession(message.getChannel()))
                    .doOnSubscribe(subscription -> endSessionList.computeIfAbsent(sessionId, key -> subscription))
                    .subscribe();
        }
    }

    private void closeSession(String endChannel) {
        UUID endSessionId = UUID.fromString(endChannel.split("_END")[0]);

        if (subscribeSessionList.containsKey(endSessionId)) {
            subscribeSessionList.remove(endSessionId).cancel();
        }
        if (endSessionList.containsKey(endSessionId)) {
            endSessionList.remove(endSessionId).cancel();
        }
    }
}

