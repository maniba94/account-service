package com.maniba.eventledger.account.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class BalanceResponse {
    private String accountId;
    private BigDecimal balance;
    private String currency;

    public BalanceResponse() {}

    public BalanceResponse(String accountId, BigDecimal balance, String currency) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
    }

}
