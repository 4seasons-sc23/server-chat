package com.instream.chatSync.domain.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Mockito.doNothing().when(messageStorageService).addMessage(any(UUID.class), anyString());
        Mockito.doNothing().when(messageStorageService).addPublishFlux(any(UUID.class));

        // then
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        ConcurrentHashMap<UUID, Disposable> subscribeSessionList = (ConcurrentHashMap<UUID, Disposable>) getField(chatService, "subscribeSessionList");
        assertTrue(subscribeSessionList.containsKey(sessionId));

        verify(reactiveStringRedisTemplate).listenTo(topic);
        verify(messageStorageService).addMessage(any(UUID.class), anyString());
        verify(messageStorageService).addPublishFlux(sessionId);
    }

    @Test
    @DisplayName("postConnection 메서드는 각 SessionId로 Redis 구독을 이미 했다면 새로운 Redis 구독을 하지 않는다.")
    public void testPostConnectionTwiceButSubscribeOnlyOne() {
        // given
        UUID sessionId = UUID.randomUUID();
        ChannelTopic topic = new ChannelTopic(sessionId.toString());
        ReactiveSubscription.Message<String, String> mockMessage = mock(ReactiveSubscription.Message.class);
        ConcurrentHashMap<UUID, Disposable> subscribeSessionList = (ConcurrentHashMap<UUID, Disposable>) getField(chatService, "subscribeSessionList");

        // when
        Mockito.when(mockMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(reactiveStringRedisTemplate.listenTo(any(ChannelTopic.class))).thenAnswer(invocation -> Flux.just(mockMessage));
        Mockito.doNothing().when(messageStorageService).addMessage(any(UUID.class), anyString());
        Mockito.doNothing().when(messageStorageService).addPublishFlux(any(UUID.class));


        // then
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));

        Disposable subscription = subscribeSessionList.get(sessionId);
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));
        assertEquals(subscribeSessionList.get(sessionId), subscription);

        verify(reactiveStringRedisTemplate).listenTo(topic);
        verify(messageStorageService).addMessage(any(UUID.class), anyString());
        verify(messageStorageService).addPublishFlux(sessionId);
    }

    @DisplayName("Multi-threading 환경에서도 postConnection 메서드는 각 SessionId로 Redis 구독을 이미 했다면 새로운 Redis 구독을 하지 않는다.")
    @Timeout(10)
    @RepeatedTest(value = 1000, name = "Thread {currentRepetition}/{totalRepetitions}")
    @Execution(ExecutionMode.CONCURRENT)
    public void testPostConnectionTwiceButSubscribeOnlyOneWhenConcurrent() {
        // given
        UUID sessionId = UUID.randomUUID();
        ChannelTopic topic = new ChannelTopic(sessionId.toString());
        ReactiveSubscription.Message<String, String> mockMessage = mock(ReactiveSubscription.Message.class);
        ConcurrentHashMap<UUID, Disposable> subscribeSessionList = (ConcurrentHashMap<UUID, Disposable>) getField(chatService, "subscribeSessionList");

        // when
        Mockito.when(mockMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(reactiveStringRedisTemplate.listenTo(any(ChannelTopic.class))).thenAnswer(invocation -> Flux.just(mockMessage));
        Mockito.doNothing().when(messageStorageService).addMessage(any(UUID.class), anyString());
        Mockito.doNothing().when(messageStorageService).addPublishFlux(any(UUID.class));


        // then
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));

        Disposable disposable = subscribeSessionList.get(sessionId);
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));
        assertEquals(subscribeSessionList.get(sessionId), disposable);

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
