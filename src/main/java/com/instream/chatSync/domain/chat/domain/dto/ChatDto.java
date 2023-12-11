package com.instream.chatSync.domain.chat.domain.dto;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatDto {
    private String id;

    private String nickname;

    private String profileImgUrl;

    private String message;

    private LocalDateTime createdAt;
}
