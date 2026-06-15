package com.maniba.eventledger.account.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
public class TransactionResponse {
    private String eventId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private Instant eventTimestamp;

    public TransactionResponse() {}

    public TransactionResponse(String eventId, String type, BigDecimal amount, String currency, Instant eventTimestamp) {
        this.eventId = eventId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
    }

}
