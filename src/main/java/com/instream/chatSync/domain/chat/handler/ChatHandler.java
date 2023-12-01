package com.instream.chatSync.domain.chat.handler;

import com.instream.chatSync.domain.chat.domain.dto.request.ChatConnectRequestDto;
import com.instream.chatSync.domain.chat.service.ChatService;
import com.instream.chatSync.domain.error.infra.enums.CommonHttpErrorCode;
import com.instream.chatSync.domain.error.model.exception.RestApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ChatHandler {
    private final ChatService chatService;

    @Autowired
    public ChatHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    public Mono<ServerResponse> postConnection(ServerRequest request) {
        return request.bodyToMono(ChatConnectRequestDto.class)
            .onErrorMap(throwable -> new RestApiException(CommonHttpErrorCode.BAD_REQUEST))
            .flatMap(chatService::postConnection)
            .then(ServerResponse.status(HttpStatus.CREATED).build());
    }

    public Mono<ServerResponse> getMessageList(ServerRequest request) {
        String sessionId = request.pathVariable("sessionId");
        return ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(chatService.streamMessages(sessionId), ServerSentEvent.class);
    }
}
