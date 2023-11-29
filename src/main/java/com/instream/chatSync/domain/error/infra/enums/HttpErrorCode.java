package com.instream.chatSync.domain.error.infra.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public interface HttpErrorCode {
    @JsonIgnore
    HttpStatus getHttpStatus();

    @JsonProperty("code")
    String getCode();

    @JsonProperty("message")
    String getMessage();
}
