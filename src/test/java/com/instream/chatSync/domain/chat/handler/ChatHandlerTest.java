package com.instream.chatSync.domain.chat.handler;

import com.instream.chatSync.domain.chat.domain.dto.ChatDto;
import com.instream.chatSync.domain.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@WebFluxTest
@Import(ChatHandler.class)
@DisplayName("ChatHandler Tests")
public class ChatHandlerTest {
    @Autowired
    private ChatHandler chatHandler;

    @MockBean
    private ChatService chatService;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setUp() {
        RouterFunction<ServerResponse> routerFunction = RouterFunctions.route(
                RequestPredicates.GET("/v1/chats/sse-connect/{sessionId}"),
                chatHandler::getMessageList);

        this.webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
    }

    @Test
    @DisplayName("GET /api/v1/chats/sse-connect/{sessionId} 호출하고 3개의 채팅 메세지를 보냈을 때, SSE 소켓을 올바르게 받고 3개의 채팅 메세지를 받을 수 있어야 한다.")
    public void responseSseSocketWithMessages() {
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
        WebTestClient.ResponseSpec result = webTestClient.get()
                .uri("/v1/chats/sse-connect/{sessionId}", sessionId)
                .exchange();

        // Then
        Mockito.verify(chatService).postConnection(sessionId);
        Mockito.verify(chatService).streamMessages(sessionId);

        result
                .expectStatus().isOk()
                .expectHeader().contentType("text/event-stream;charset=UTF-8")
                .expectBodyList(ServerSentEvent.class)
                .hasSize(3);
    }

    @Test
    @DisplayName("WebTestClient를 사용하지 않고 테스트를 진행하려고 하면 PathVariable 파싱이 안되는 에러가 발생한다.")
    public void throwIllegalArgumentExceptionWhenNotUsingWebTestClient() {
        // Given
        UUID sessionId = UUID.randomUUID();
        MockServerHttpRequest mockRequest = MockServerHttpRequest.get("/v1/chats/sse-connect/" + sessionId)
                .build();
        ServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);
        ServerRequest serverRequest = ServerRequest.create(mockExchange, HandlerStrategies.withDefaults().messageReaders());

        assertThrows(IllegalArgumentException.class, () -> serverRequest.pathVariable("sessionId"));
    }
}
