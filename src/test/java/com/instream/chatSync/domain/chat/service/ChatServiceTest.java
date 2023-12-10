package com.instream.chatSync.domain.chat.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebFluxTest(ChatService.class)
public class ChatServiceTest {

    @MockBean
    private ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate;

    @MockBean
    private MessageStorageService messageStorageService;

    @Autowired
    private ChatService chatService;

    private UUID sessionId = UUID.randomUUID();

    private ChannelTopic chatMessageTopic;

    private ChannelTopic closeTopic;

    private ConcurrentHashMap<UUID, Subscription> subscribeSessionList;

    private ConcurrentHashMap<UUID, Subscription> endSessionList;

    @BeforeEach
    public void setUp() {
        chatMessageTopic = new ChannelTopic(sessionId.toString());
        closeTopic = new ChannelTopic(sessionId + "_END");
        subscribeSessionList = (ConcurrentHashMap<UUID, Subscription>) getField(chatService, "subscribeSessionList");
        endSessionList = (ConcurrentHashMap<UUID, Subscription>) getField(chatService, "endSessionList");

        subscribeSessionList.clear();
        endSessionList.clear();
    }

    @Test
    @DisplayName("postConnection 메서드는 sessionId, sessionId_END에 대해 각각 구독을 1개씩 생성해야 한다.")
    public void testPostConnectionWhenSubscribe() {
        // given
        ReactiveSubscription.Message<String, String> mockChatMessage = mock(ReactiveSubscription.Message.class);
        ReactiveSubscription.Message<String, String> mockCloseMessage = mock(ReactiveSubscription.Message.class);

        // when
        Mockito.when(mockChatMessage.getChannel()).thenReturn(chatMessageTopic.getTopic());
        Mockito.when(mockChatMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(mockCloseMessage.getChannel()).thenReturn(closeTopic.getTopic());
        Mockito.when(mockCloseMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(reactiveStringRedisTemplate.listenTo(chatMessageTopic)).thenAnswer(invocation -> Flux.just(mockChatMessage));
        Mockito.when(reactiveStringRedisTemplate.listenTo(closeTopic)).thenAnswer(invocation -> Flux.empty());
        Mockito.doNothing().when(messageStorageService).addMessage(any(UUID.class), anyString());
        Mockito.doNothing().when(messageStorageService).addPublishFlux(any(UUID.class));

        // then
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));
        assertTrue(endSessionList.containsKey(sessionId));

        verifyThreadSafeResult(sessionId, chatMessageTopic, closeTopic);
    }

    @Test
    @DisplayName("postConnection 메서드는 sessionId, sessionId_END에 대해 각각 구독을 1개씩 생성하고, 1초 이후 구독을 해제할 수 있어야 한다.")
    public void testPostConnectionWhenDisposeAfterSubscribe() throws InterruptedException {
        // given
        ReactiveSubscription.Message<String, String> mockChatMessage = mock(ReactiveSubscription.Message.class);
        ReactiveSubscription.Message<String, String> mockCloseMessage = mock(ReactiveSubscription.Message.class);

        // when
        Mockito.when(mockChatMessage.getChannel()).thenReturn(chatMessageTopic.getTopic());
        Mockito.when(mockChatMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(mockCloseMessage.getChannel()).thenReturn(closeTopic.getTopic());
        Mockito.when(mockCloseMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(reactiveStringRedisTemplate.listenTo(chatMessageTopic)).thenAnswer(invocation -> Flux.just(mockChatMessage));
        Mockito.when(reactiveStringRedisTemplate.listenTo(closeTopic)).thenAnswer(invocation -> Flux.just(mockCloseMessage).delayElements(Duration.ofSeconds(1)));
        Mockito.doNothing().when(messageStorageService).addMessage(any(UUID.class), anyString());
        Mockito.doNothing().when(messageStorageService).addPublishFlux(any(UUID.class));

        // then
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        Thread.sleep(2000);

        assertFalse(subscribeSessionList.containsKey(sessionId));
        assertFalse(endSessionList.containsKey(sessionId));

        verifyThreadSafeResult(sessionId, chatMessageTopic, closeTopic);
    }

    @Test
    @DisplayName("postConnection 메서드는 sessionId, sessionId_END에 대해 구독 생성과 구독 해제 메세지가 동시에 도착했을 때도 오류가 없어야 한다.")
    public void testPostConnectionGetMessagesConcurrent() {
        // given
        ReactiveSubscription.Message<String, String> mockChatMessage = mock(ReactiveSubscription.Message.class);
        ReactiveSubscription.Message<String, String> mockCloseMessage = mock(ReactiveSubscription.Message.class);

        // when
        Mockito.when(mockChatMessage.getChannel()).thenReturn(chatMessageTopic.getTopic());
        Mockito.when(mockChatMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(mockCloseMessage.getChannel()).thenReturn(closeTopic.getTopic());
        Mockito.when(mockCloseMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(reactiveStringRedisTemplate.listenTo(chatMessageTopic)).thenAnswer(invocation -> Flux.just(mockChatMessage));
        Mockito.when(reactiveStringRedisTemplate.listenTo(closeTopic)).thenAnswer(invocation -> Flux.just(mockCloseMessage));
        Mockito.doNothing().when(messageStorageService).addMessage(any(UUID.class), anyString());
        Mockito.doNothing().when(messageStorageService).addPublishFlux(any(UUID.class));

        // then
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertFalse(subscribeSessionList.containsKey(sessionId));
        assertFalse(endSessionList.containsKey(sessionId));

        verifyThreadSafeResult(sessionId, chatMessageTopic, closeTopic);
    }

    @Test
    @DisplayName("postConnection 메서드는 각 SessionId로 Redis 구독을 이미 했다면 새로운 Redis 구독을 하지 않는다.")
    public void testPostConnectionTwiceButSubscribeOnlyOne() {
        // given
        ReactiveSubscription.Message<String, String> mockChatMessage = mock(ReactiveSubscription.Message.class);
        ReactiveSubscription.Message<String, String> mockCloseMessage = mock(ReactiveSubscription.Message.class);
        Subscription chatMessageSubscription;
        Subscription closeSubscription;

        // when
        Mockito.when(mockChatMessage.getChannel()).thenReturn(chatMessageTopic.getTopic());
        Mockito.when(mockChatMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(mockCloseMessage.getChannel()).thenReturn(closeTopic.getTopic());
        Mockito.when(mockCloseMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(reactiveStringRedisTemplate.listenTo(chatMessageTopic)).thenAnswer(invocation -> Flux.just(mockChatMessage));
        Mockito.when(reactiveStringRedisTemplate.listenTo(closeTopic)).thenAnswer(invocation -> Flux.empty());
        Mockito.doNothing().when(messageStorageService).addMessage(any(UUID.class), anyString());
        Mockito.doNothing().when(messageStorageService).addPublishFlux(any(UUID.class));


        // then
        // 한 sessionId에 대해 1회 구독
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));
        assertTrue(endSessionList.containsKey(sessionId));

        chatMessageSubscription = subscribeSessionList.get(sessionId);
        closeSubscription = endSessionList.get(sessionId);

        // 한 sessionId에 대해 2회 구독 시도
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));
        assertTrue(endSessionList.containsKey(sessionId));

        assertEquals(subscribeSessionList.get(sessionId), chatMessageSubscription);
        assertEquals(endSessionList.get(sessionId), closeSubscription);

        verifyThreadSafeResult(sessionId, chatMessageTopic, closeTopic);
    }

    @DisplayName("Multi-threading 환경에서도 postConnection 메서드는 각 SessionId로 Redis 구독을 이미 했다면 새로운 Redis 구독을 하지 않는다.")
    @Timeout(10)
    @RepeatedTest(value = 1000, name = "Thread {currentRepetition}/{totalRepetitions}")
    @Execution(ExecutionMode.CONCURRENT)
    public void testPostConnectionIsSafeWhenConcurrent() {
        // given
        ReactiveSubscription.Message<String, String> mockChatMessage = mock(ReactiveSubscription.Message.class);
        ReactiveSubscription.Message<String, String> mockCloseMessage = mock(ReactiveSubscription.Message.class);
        Subscription chatMessageSubscription;
        Subscription closeSubscription;

        // when
        Mockito.when(mockChatMessage.getChannel()).thenReturn(chatMessageTopic.getTopic());
        Mockito.when(mockChatMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(mockCloseMessage.getChannel()).thenReturn(closeTopic.getTopic());
        Mockito.when(mockCloseMessage.getMessage()).thenReturn("testMessage");
        Mockito.when(reactiveStringRedisTemplate.listenTo(chatMessageTopic)).thenAnswer(invocation -> Flux.just(mockChatMessage));
        Mockito.when(reactiveStringRedisTemplate.listenTo(closeTopic)).thenAnswer(invocation -> Flux.empty());
        Mockito.doNothing().when(messageStorageService).addMessage(any(UUID.class), anyString());
        Mockito.doNothing().when(messageStorageService).addPublishFlux(any(UUID.class));

        // then
        // 한 sessionId에 대해 1회 구독
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));
        assertTrue(endSessionList.containsKey(sessionId));

        chatMessageSubscription = subscribeSessionList.get(sessionId);
        closeSubscription = endSessionList.get(sessionId);

        // 한 sessionId에 대해 2회 구독 시도
        StepVerifier.create(chatService.postConnection(sessionId))
                .verifyComplete();

        assertTrue(subscribeSessionList.containsKey(sessionId));
        assertTrue(endSessionList.containsKey(sessionId));

        assertEquals(subscribeSessionList.get(sessionId), chatMessageSubscription);
        assertEquals(endSessionList.get(sessionId), closeSubscription);

        verifyThreadSafeResult(sessionId, chatMessageTopic, closeTopic);
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

    private void verifyThreadSafeResult(UUID sessionId, ChannelTopic chatMessageTopic, ChannelTopic closeTopic) {
        verify(reactiveStringRedisTemplate).listenTo(chatMessageTopic);
        verify(reactiveStringRedisTemplate).listenTo(closeTopic);
        verify(messageStorageService).addMessage(any(UUID.class), anyString());
        verify(messageStorageService).addPublishFlux(sessionId);
    }
}
