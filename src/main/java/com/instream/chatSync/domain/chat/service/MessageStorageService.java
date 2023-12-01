package com.instream.chatSync.domain.chat.service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MessageStorageService {
    private final ConcurrentHashMap<String, Queue<List<String>>> messageQueues = new ConcurrentHashMap<>();

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
            .map(tick -> {
                Queue<List<String>> msgQueue = messageQueues.getOrDefault(sessionId, new ConcurrentLinkedQueue<>());
                List<String> messages = msgQueue.poll();
                if (messages == null) {
                    messages = new ArrayList<>(); // 새로운 빈 배열 생성
                }
                msgQueue.add(new ArrayList<>()); // 다음 메시지를 위한 새 빈 배열 큐에 추가
                return messages;
            })
            .map(batch -> ServerSentEvent.<List<String>>builder().data(batch).build());
    }
}
