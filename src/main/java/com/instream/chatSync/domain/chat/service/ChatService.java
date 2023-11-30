package com.instream.chatSync.domain.chat.service;

import com.instream.chatSync.domain.chat.domain.dto.SubscriptionRegistry;
import com.instream.chatSync.domain.chat.domain.dto.request.ChatConnectRequestDto;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ChatService {
    @Autowired
    private RedisMessageListenerContainer redisContainer;

    @Autowired
    private MessageListenerAdapter messageListener;

    @Autowired
    private SubscriptionRegistry subscriptionRegistry;

    public void subscribeToSession(String sessionId, String participantId) {
        // 구독자 추가
        subscriptionRegistry.addSubscriber(sessionId, participantId);
    }

    public void unsubscribeFromSession(String sessionId, String participantId) {
        // 구독자 제거
        subscriptionRegistry.removeSubscriber(sessionId, participantId);
    }

    public List<String> getSessionSubscribers(String sessionId) {
        // 특정 세션의 구독자 목록 조회
        return subscriptionRegistry.getSubscribers(sessionId);
    }

    public Mono<Void> postConnection(ChatConnectRequestDto connectRequestDto) {
        ChannelTopic topic = new ChannelTopic(connectRequestDto.sessionId());
        this.subscribeToSession(connectRequestDto.sessionId(), connectRequestDto.participantId());
        redisContainer.addMessageListener(messageListener, topic);
        System.out.println(getSessionSubscribers(connectRequestDto.sessionId()));
        return Mono.empty();
    }
}
