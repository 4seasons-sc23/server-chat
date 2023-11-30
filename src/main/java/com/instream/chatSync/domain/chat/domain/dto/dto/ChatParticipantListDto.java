package com.instream.chatSync.domain.chat.domain.dto.dto;

import java.util.List;

public record ChatParticipantListDto(
    String sessionId,
    List<String> participantId
) {}
