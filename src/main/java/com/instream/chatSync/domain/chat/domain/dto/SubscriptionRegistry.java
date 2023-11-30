package com.instream.chatSync.domain.chat.domain.dto;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRegistry {
    private final ConcurrentHashMap<String, List<String>> subscriptions = new ConcurrentHashMap<>();

    public void addSubscriber(String sessionId, String participantId) {
        subscriptions.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(participantId);
    }

    public void removeSubscriber(String sessionId, String participantId) {
        if (subscriptions.containsKey(sessionId)) {
            subscriptions.get(sessionId).remove(participantId);
        }
    }

    public List<String> getSubscribers(String sessionId) {
        return subscriptions.getOrDefault(sessionId, new ArrayList<>());
    }
}
