package com.instream.chatSync.domain.error.handler;

import com.instream.chatSync.domain.error.infra.enums.CommonHttpErrorCode;
import com.instream.chatSync.domain.error.infra.enums.HttpErrorCode;
import com.instream.chatSync.domain.error.model.exception.RestApiException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.EncodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2) // webflux default @Order(-1)
@Slf4j
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    private List<HttpMessageWriter<?>> messageWriters;

    @Autowired
    public GlobalErrorWebExceptionHandler(ServerCodecConfigurer serverCodecConfigurer) {
        this.messageWriters = serverCodecConfigurer.getWriters();
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof RestApiException restApiException) {
            log.error("handleRestApiException", ex);
            return writeResponse(restApiException.getHttpErrorCode(), exchange);
        }
        if (ex instanceof RestClientException) {
            log.error("handleRestClientException", ex);
            return writeResponse(CommonHttpErrorCode.SERVICE_UNAVAILABLE, exchange);
        }
        if (ex instanceof ResponseStatusException) {
            log.error("handleResponseStatusException", ex);
            return writeResponse(((ResponseStatusException) ex), exchange);
        }
        log.error("handleException", ex);
        return writeResponse(CommonHttpErrorCode.INTERNAL_SERVER_ERROR, exchange);
    }

    private Mono<Void> writeResponse(HttpErrorCode httpErrorCode, ServerWebExchange exchange) {
        return Mono.defer(() -> {
            ResolvableType elementType = ResolvableType.forInstance(httpErrorCode);
            @SuppressWarnings("unchecked")
            HttpMessageWriter<HttpErrorCode> messageWriter = (HttpMessageWriter<HttpErrorCode>) this.messageWriters.stream()
                    .filter(writer -> writer.canWrite(elementType, MediaType.APPLICATION_JSON))
                    .findFirst()
                    .orElseThrow(() -> new EncodingException("No suitable HttpMessageWriter found"));

            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().setStatusCode(httpErrorCode.getHttpStatus());

            return messageWriter.write(Mono.just(httpErrorCode), elementType, MediaType.APPLICATION_JSON, exchange.getResponse(), Collections.emptyMap());
        });
    }

    private Mono<Void> writeResponse(ResponseStatusException responseStatusException, ServerWebExchange exchange) {
        return Mono.defer(() -> {
            HttpErrorCode httpErrorCode = new HttpErrorCode() {
                @Override
                public HttpStatus getHttpStatus() {
                    return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
                }

                @Override
                public String getCode() {
                    return responseStatusException.getMessage();
                }

                @Override
                public String getMessage() {
                    return responseStatusException.getReason();
                }
            };
            ResolvableType elementType = ResolvableType.forInstance(httpErrorCode);
            @SuppressWarnings("unchecked")
            HttpMessageWriter<HttpErrorCode> messageWriter = (HttpMessageWriter<HttpErrorCode>) this.messageWriters.stream()
                    .filter(writer -> writer.canWrite(elementType, MediaType.APPLICATION_JSON))
                    .findFirst()
                    .orElseThrow(() -> new EncodingException("No suitable HttpMessageWriter found"));

            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().setStatusCode(responseStatusException.getStatusCode());

            return messageWriter.write(Mono.just(httpErrorCode), elementType, MediaType.APPLICATION_JSON, exchange.getResponse(), Collections.emptyMap());
        });
    }
}
