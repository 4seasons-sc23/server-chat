package com.instream.chatSync.domain.chat.service;

import com.instream.chatSync.domain.chat.domain.dto.request.ChatBillingRequestDto;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MessageStorageService {
    private final ConcurrentHashMap<String, Queue<List<String>>> messageQueues = new ConcurrentHashMap<>();
    private final BillingService billingService;

    public MessageStorageService(BillingService billingService) {
        this.billingService = billingService;
    }

    public void addMessage(String sessionId, String message) {
        Queue<List<String>> sessionQueue = messageQueues.computeIfAbsent(sessionId, k -> {
            Queue<List<String>> newQueue = new ConcurrentLinkedQueue<>();
            newQueue.add(new ArrayList<>()); // 초기 빈 배열 추가
            return newQueue;
        });

        List<String> currentMessages = sessionQueue.peek();

        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
            sessionQueue.add(currentMessages);
        }

        currentMessages.add(message);
//        System.out.println(currentMessages);
    }

    public Flux<ServerSentEvent<List<String>>> streamMessages(String sessionId) {
        return Flux.interval(Duration.ofSeconds(1))
            .flatMap(tick -> {
                Queue<List<String>> msgQueue = messageQueues.getOrDefault(sessionId, new ConcurrentLinkedQueue<>());
                List<String> messages = msgQueue.poll();
                if (messages == null) {
                    messages = new ArrayList<>();
                }
                msgQueue.add(new ArrayList<>()); // 다음 메시지를 위한 새 빈 배열 큐에 추가

                ChatBillingRequestDto billingRequestDto = ChatBillingRequestDto.builder()
                    .sessionId(UUID.fromString(sessionId))
                    .count(messages.size())
                    .build();

                return billingService.postChatBilling(billingRequestDto)
                    .thenReturn(ServerSentEvent.<List<String>>builder().data(messages).build());
            });
    }
}
