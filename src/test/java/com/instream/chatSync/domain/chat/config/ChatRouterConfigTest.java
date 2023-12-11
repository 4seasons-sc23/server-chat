package com.instream.chatSync.domain.chat.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instream.chatSync.core.config.WebConfig;
import com.instream.chatSync.domain.chat.domain.dto.ChatDto;
import com.instream.chatSync.domain.chat.handler.ChatHandler;
import com.instream.chatSync.domain.chat.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@WebFluxTest
@Import({WebConfig.class, ChatConfig.class, ChatHandler.class})
@DisplayName("Chat Router Configuration Tests")
public class ChatRouterConfigTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ChatService chatService;

    @Test
    @DisplayName("GET /api/v1/chats/sse-connect/{sessionId} 호출하고 3개의 채팅 메세지를 받았을 때")
    public void getSseSocketWithMessages() {
        // Given
        UUID sessionId = UUID.randomUUID();
        List<ServerSentEvent<ChatDto>> sseMessages = List.of(
                ServerSentEvent.builder(new ChatDto("1", "nickname1", "profileUrl1", "메시지 1", LocalDateTime.now())).build(),
                ServerSentEvent.builder(new ChatDto("2", "nickname2", "profileUrl2", "메시지 2", LocalDateTime.now())).build(),
                ServerSentEvent.builder(new ChatDto("3", "nickname3", "profileUrl3", "메시지 3", LocalDateTime.now())).build()
        );
        Mockito.when(chatService.postConnection(sessionId))
                .thenReturn(Mono.empty());
        Mockito.when(chatService.streamMessages(sessionId))
                .thenAnswer(invocation -> Flux.fromIterable(sseMessages));
        // When
        FluxExchangeResult<String> result = webTestClient.get()
                .uri("/v1/chats/sse-connect/{sessionId}", sessionId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/event-stream")
                .returnResult(String.class);

        // Then
        Mockito.verify(chatService).postConnection(sessionId);
        Mockito.verify(chatService).streamMessages(sessionId);

        StepVerifier.create(result.getResponseBody())
                .expectNextMatches(jsonContainsMessage("메시지 1"))
                .expectNextMatches(jsonContainsMessage("메시지 2"))
                .expectNextMatches(jsonContainsMessage("메시지 3"))
                .thenCancel()
                .verify();
    }

    private Predicate<String> jsonContainsMessage(String message) {
        return json -> {
            try {
                ChatDto chatDto = objectMapper.readValue(json, ChatDto.class);
                return message.equals(chatDto.getMessage());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return false;
            }
        };
    }
}
