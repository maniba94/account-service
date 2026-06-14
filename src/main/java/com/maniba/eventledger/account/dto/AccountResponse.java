package com.maniba.eventledger.account.dto;

import java.math.BigDecimal;
import java.util.List;

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

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<TransactionResponse> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionResponse> transactions) { this.transactions = transactions; }
}
