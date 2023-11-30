package com.instream.chatSync.domain.chat.domain.dto.dto;

import java.util.List;

public record ChatParticipantDto(
    String sessionId,
    List<String> participantId
) {}
