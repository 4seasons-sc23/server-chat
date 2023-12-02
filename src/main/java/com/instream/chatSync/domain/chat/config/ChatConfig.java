package com.instream.chatSync.domain.chat.config;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import com.instream.chatSync.domain.chat.handler.ChatHandler;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ChatConfig {
    @Bean
    public RouterFunction<ServerResponse> v1ChatRoutes(
        ChatHandler chatHandler) {
        return route().nest(RequestPredicates.path("/v1/chats"),
            builder -> {
                builder.add(getMessageList(chatHandler));
            },
            ops -> ops.operationId("919")
        ).build();
    }

        private RouterFunction<ServerResponse> getMessageList(ChatHandler chatHandler) {
            return route()
                .GET(
                    "/sse-connect/{sessionId}",
                    chatHandler::getMessageList,
                    ops -> ops.operationId("919")
                        .parameter(
                            parameterBuilder().name("sessionId").in(ParameterIn.PATH).required(true)
                                .example("14d38654-89cb-11ee-9aae-0242ac140002"))
                        .response(responseBuilder().responseCode(HttpStatus.OK.name()))
                )
                .build();
        }
}
