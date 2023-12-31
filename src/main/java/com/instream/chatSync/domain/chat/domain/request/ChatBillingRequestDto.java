package com.instream.chatSync.domain.chat.domain.request;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ChatBillingRequestDto(
    UUID sessionId,
    double count
) {

}
