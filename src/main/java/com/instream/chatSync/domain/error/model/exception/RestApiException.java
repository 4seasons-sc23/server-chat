package com.instream.chatSync.domain.error.model.exception;

import com.instream.chatSync.domain.error.infra.enums.HttpErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString
public class RestApiException extends RuntimeException {
    private final HttpErrorCode httpErrorCode;
}
