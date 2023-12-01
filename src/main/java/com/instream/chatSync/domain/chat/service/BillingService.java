package com.instream.chatSync.domain.chat.service;

import com.instream.chatSync.domain.chat.domain.dto.request.ChatBillingRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BillingService {

    private final WebClient webClient;

    @Value("${tenant.base.url}")
    private String TENANT_BASE_URL;

    @Autowired
    public BillingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(TENANT_BASE_URL).build();
    }

    public Mono<HttpStatusCode> postChatBilling(ChatBillingRequestDto chatBillingRequestDto) {
        return webClient.post()
            .uri("/v1/billings")
            .body(Mono.just(chatBillingRequestDto), ChatBillingRequestDto.class)
            .exchangeToMono(response -> Mono.just(response.statusCode()));
    }
}
