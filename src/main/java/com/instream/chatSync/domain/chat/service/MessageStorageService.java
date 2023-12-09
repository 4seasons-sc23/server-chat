package com.instream.chatSync.domain.chat.service;

import com.instream.chatSync.domain.chat.domain.request.ChatBillingRequestDto;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
@Slf4j
public class MessageStorageService {
    private final ConcurrentHashMap<String, Queue<String>> messageQueues = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Disposable> messagePublishFluxes = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<Sinks.Many<ServerSentEvent<List<String>>>>> sessionSockets = new ConcurrentHashMap<>();

    private final BillingService billingService;

    public MessageStorageService(BillingService billingService) {
        this.billingService = billingService;
    }

    public void addMessage(String sessionId, String message) {
        Queue<String> sessionQueue = messageQueues.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
        sessionQueue.add(message);
    }

    public void addPublishFlux(String sessionId) {
        if (messagePublishFluxes.containsKey(sessionId)) {
            return;
        }
        Flux<Void> flux = Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> {
                    Queue<String> msgQueue = messageQueues.get(sessionId);

                    if (msgQueue == null || msgQueue.isEmpty()) {
                        return Mono.empty();
                    }

                    // 임시 메세지 저장소 생성
                    List<String> tempMessages = new ArrayList<>(msgQueue);

                    // messageQueue를 비워줍니다. 채팅 편집 동시성을 높이기 위해서 원본은 사용하지 않습니다.
                    msgQueue.clear();

                    billingChat(sessionId, tempMessages);

                    // sessionId 해당하는 소켓에 메시지 전송
                    ServerSentEvent<List<String>> event = ServerSentEvent.builder(tempMessages).build();
                    sessionSockets.getOrDefault(sessionId, new ArrayList<>())
                            .parallelStream()
                            .forEach(socket -> socket.tryEmitNext(event));

                    // 임시 메시지 큐 메모리 해제
                    tempMessages.clear();
                    return Mono.empty();
                });

        log.info("Create message publisher {}", sessionId);

        messagePublishFluxes.put(sessionId, flux.subscribe());


    }

    public Flux<ServerSentEvent<List<String>>> streamMessages(String sessionId) {
        Sinks.Many<ServerSentEvent<List<String>>> sink = Sinks.many().multicast().directAllOrNothing();
        List<Sinks.Many<ServerSentEvent<List<String>>>> sockets = sessionSockets.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        sockets.add(sink);

        log.info("Create SSE {}", sessionId);

        return sink.asFlux();
    }

    private void billingChat(String sessionId, List<String> tempMessages) {
        ChatBillingRequestDto billingRequestDto = ChatBillingRequestDto.builder()
                .sessionId(UUID.fromString(sessionId))
                .count(tempMessages.size())
                .build();
        billingService.postChatBilling(billingRequestDto).subscribe();
    }
}
