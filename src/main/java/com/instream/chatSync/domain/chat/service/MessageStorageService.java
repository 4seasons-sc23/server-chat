package com.instream.chatSync.domain.chat.service;

import com.instream.chatSync.domain.chat.domain.request.ChatBillingRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class MessageStorageService {
    private final ConcurrentHashMap<UUID, Queue<String>> messageQueues = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Disposable> messagePublishFluxes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Sinks.Many<ServerSentEvent<List<String>>>>> sessionSockets = new ConcurrentHashMap<>();

    private final BillingService billingService;

    @Autowired
    public MessageStorageService(BillingService billingService) {
        this.billingService = billingService;
    }

    public void addMessage(UUID sessionId, String message) {
        Queue<String> sessionQueue = messageQueues.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        sessionQueue.add(message);
    }

    public void addPublishFlux(UUID sessionId) {
        if (messagePublishFluxes.containsKey(sessionId)) {
            return;
        }
        Flux<Long> flux = Flux.interval(Duration.ofMillis(24)).doOnNext(tick -> publishMessages(sessionId));

        messagePublishFluxes.computeIfAbsent(sessionId, key -> {
            log.info("Create message publisher {}", sessionId);
            return flux.subscribe();
        });
    }

    public void closePublishFlux(UUID sessionId) {
        if (!messagePublishFluxes.containsKey(sessionId)) {
            return;
        }
        Disposable disposable = messagePublishFluxes.get(sessionId);

        if (disposable.isDisposed()) {
            return;
        }

        disposable.dispose();
    }

    public Flux<ServerSentEvent<List<String>>> streamMessages(UUID sessionId) {
        Sinks.Many<ServerSentEvent<List<String>>> sink = Sinks.many().multicast().directAllOrNothing();
        List<Sinks.Many<ServerSentEvent<List<String>>>> sockets = sessionSockets.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        sockets.add(sink);

        log.info("Create SSE {}", sessionId);

        return sink.asFlux();
    }

    private void publishMessages(UUID sessionId) {
        Queue<String> msgQueue = messageQueues.get(sessionId);

        if (msgQueue == null || msgQueue.isEmpty()) {
            return;
        }

        // 임시 메시지 저장소 생성 및 이벤트 생성
        List<String> tempMessages = new ArrayList<>(msgQueue);
        ServerSentEvent<List<String>> event = ServerSentEvent.builder(tempMessages).build();
        Mono<Void> deleteMessageFromMsgQueueMono = deleteMessagesFromQueue(msgQueue, tempMessages.size());
        Mono<Void> billingChatMono = billingChat(sessionId, tempMessages);
        Mono<Void> broadcastingChatMono = broadcastChat(sessionId, event);

        // 모든 Reactive 작업 실행 후, 임시 메시지 큐 메모리 해제
        Mono.when(deleteMessageFromMsgQueueMono, billingChatMono, broadcastingChatMono)
                .then(Mono.fromRunnable(tempMessages::clear))
                .subscribe();
    }

    private Mono<Void> deleteMessagesFromQueue(Queue<String> msgQueue, int size) {
        return Mono.fromRunnable(() -> {
            for (int i = 0; i < size; i++) {
                msgQueue.poll();
            }
        });
    }

    private Mono<Void> billingChat(UUID sessionId, List<String> tempMessages) {
        ChatBillingRequestDto billingRequestDto = ChatBillingRequestDto.builder()
                .sessionId(sessionId)
                .count(tempMessages.size())
                .build();
        return billingService.postChatBilling(billingRequestDto).then();
    }

    private Mono<Void> broadcastChat(UUID sessionId, ServerSentEvent<List<String>> event) {
        return Flux.fromIterable(sessionSockets.getOrDefault(sessionId, new ArrayList<>()))
                .parallel()
                .runOn(Schedulers.parallel())
                .doOnNext(socket -> socket.tryEmitNext(event))
                .then();
    }

}
