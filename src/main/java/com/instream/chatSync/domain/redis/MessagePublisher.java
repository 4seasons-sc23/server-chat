package com.instream.chatSync.domain.redis;

public interface MessagePublisher {
    void publish(String message);
}