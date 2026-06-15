package com.maniba.eventledger.account.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
public class AccountResponse {
    private String accountId;
    private BigDecimal balance;
    private String currency;
    private List<TransactionResponse> transactions;

    public AccountResponse() {}

    public AccountResponse(String accountId, BigDecimal balance, String currency, List<TransactionResponse> transactions) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
        this.transactions = transactions;
    }

}
