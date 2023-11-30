package com.instream.chatSync.domain.chat.domain.dto.request;

import java.util.UUID;

public record ChatConnectRequestDto(
    String sessionId,
    String participantId
) {}
