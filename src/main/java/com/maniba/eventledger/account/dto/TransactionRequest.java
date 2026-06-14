package com.maniba.eventledger.account.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
public class TransactionRequest {

    // getters and setters
    @NotBlank
    private String eventId;

    @NotBlank
    private String type;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private Instant eventTimestamp;

}
