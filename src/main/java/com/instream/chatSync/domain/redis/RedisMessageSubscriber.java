package com.instream.chatSync.domain.redis;

import com.instream.chatSync.domain.chat.service.MessageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageSubscriber implements MessageListener {
    private final MessageStorageService messageStorageService;

    @Autowired
    public RedisMessageSubscriber(MessageStorageService chatService) {
        this.messageStorageService = chatService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageContent = new String(message.getBody());
        String channel = new String(message.getChannel());
        messageStorageService.addMessage(channel, messageContent);
        System.out.println("Message received: " + messageContent);
    }
}
