package com.maniba.eventledger.account.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class ErrorResponse {
    private String error;
    private String message;
    private String traceId;
    private Instant timestamp;

    public ErrorResponse() {}

    public ErrorResponse(String error, String message, String traceId, Instant timestamp) {
        this.error = error;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = timestamp;
    }

}
