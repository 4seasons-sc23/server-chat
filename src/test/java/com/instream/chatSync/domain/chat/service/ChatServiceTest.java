package com.instream.chatSync.domain.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@WebFluxTest(ChatService.class)
public class ChatServiceTest {

    @MockBean
    private ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate;

    @MockBean
    private MessageStorageService messageStorageService;

    @Autowired
    private ChatService chatService;

    @Test
    @DisplayName("postConnection 메서드는 새로운 Redis 구독을 생성해야 한다")
    public void testPostConnection() {
        // given
        UUID sessionId = UUID.randomUUID();
        ChannelTopic topic = new ChannelTopic(sessionId.toString());
        ReactiveSubscription.Message<String, String> mockMessage = mock(ReactiveSubscription.Message.class);

        // when
        Mockito.when(mockMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(reactiveStringRedisTemplate.listenTo(any(ChannelTopic.class))).thenAnswer(invocation -> Flux.just(mockMessage));
        Mockito.when(messageStorageService.addMessage(any(UUID.class), anyString())).thenReturn(Mono.empty());
        Mockito.when(messageStorageService.addPublishFlux(any(UUID.class))).thenReturn(Mono.empty());

        // then
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        ConcurrentHashMap<UUID, Disposable> subscribeSessionList = (ConcurrentHashMap<UUID, Disposable>) getField(chatService, "subscribeSessionList");
        assertTrue(subscribeSessionList.containsKey(sessionId));

        verify(reactiveStringRedisTemplate).listenTo(topic);
        verify(messageStorageService).addMessage(any(UUID.class), anyString());
        verify(messageStorageService).addPublishFlux(sessionId);
    }


    private Object getField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
