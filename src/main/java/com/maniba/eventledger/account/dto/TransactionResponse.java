package com.maniba.eventledger.account.dto;

import java.math.BigDecimal;
import java.time.Instant;

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

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
