package com.maniba.eventledger.account.controller;

import com.maniba.eventledger.account.dto.AccountResponse;
import com.maniba.eventledger.account.dto.BalanceResponse;
import com.maniba.eventledger.account.dto.TransactionRequest;
import com.maniba.eventledger.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping(path = "/{accountId}/transactions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BalanceResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionRequest request) {
        BalanceResponse response = accountService.applyTransaction(accountId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        BalanceResponse response = accountService.getBalance(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        AccountResponse response = accountService.getAccount(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/health")
    public ResponseEntity<Map<String, String>> getHealth() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
